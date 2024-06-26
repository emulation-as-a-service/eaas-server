/*
 * This file is part of the Emulation-as-a-Service framework.
 *
 * The Emulation-as-a-Service framework is free software: you can
 * redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * The Emulation-as-a-Service framework is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PARTICULAR PURPOSE.  See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the Emulation-as-a-Software framework.
 * If not, see <http://www.gnu.org/licenses/>.
 */

package de.bwl.bwfla.emil.session;

import de.bwl.bwfla.emil.Components;
import de.bwl.bwfla.emil.session.rest.DetachRequest;
import org.apache.tamaya.inject.api.Config;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.enterprise.concurrent.ManagedScheduledExecutorService;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;


@ApplicationScoped
public class SessionManager
{
	private final Logger log = Logger.getLogger("SESSION-MANAGER");

	private final Map<String, Session> sessions = new ConcurrentHashMap<String, Session>();

	@Inject
	private Components endpoint = null;

	@Inject
	@Config("components.client_timeout")
	protected Duration sessionExpirationTimeout;

	@Inject
	@Config("components.client_timeout")
	private Duration resourceExpirationTimeout;

	@Resource(lookup = "java:jboss/ee/concurrency/executor/io")
	private ExecutorService executor;

	@Resource(lookup = "java:jboss/ee/concurrency/scheduler/default")
	private ManagedScheduledExecutorService scheduler;

	/** Registers a new session */
	public void register(Session session)
	{
		sessions.put(session.id(), session);
		log.info("Session '" + session.id() + "' registered");
	}

	/** Returns session */
	public Session get(String id)
	{
		return sessions.get(id);
	}

	/** Returns a list of all session IDs */
	public Collection<Session> list()
	{
		return sessions.values()
				.stream()
				.filter(Session::isDetached)
				.collect(Collectors.toList());
	}

	/** Updates session's lifetime */
	public void setLifetime(String sid, long lifetime, TimeUnit unit, String name)
	{
		this.setLifetime(sid, lifetime, unit, name, null);
	}

	/** Updates session's lifetime */
	public void setLifetime(String sid, long lifetime, TimeUnit unit, String name, DetachRequest.ComponentTitleCreator title)
	{
		sessions.computeIfPresent(sid, (unused, session) -> {
			session.setName(name);
			if (lifetime < 0L) {
				session.setLifetime(-1L);
				session.setExpirationTimestamp(-1L);
			}
			else {
				session.setLifetime(unit.toMillis(lifetime));
				final var timestamp = (lifetime > 0L) ? SessionManager.timems() + session.getLifetime() : Long.MAX_VALUE;
				session.setExpirationTimestamp(timestamp);
			}

			if (title != null && title.getComponentName() != null) {
				final String cid = title.getComponentId();
				final var component = session.components()
						.get(cid);

				if (component != null)
					component.setCustomName(title.getComponentName());
				else log.warning("Component " + cid + " not found in session " + sid + "!");
			}

			return session;
		});
	}

	/** Send keepalive for session */
	public boolean keepalive(String id)
	{
		final var cursession = sessions.get(id);
		if (cursession == null)
			return false;

		cursession.keepalive(endpoint, log);
		return true;
	}

	/** Runs internal keepalives calls for detached session's resources */
	public void update(ExecutorService executor)
	{
		final List<String> idsToRemove = new ArrayList<String>();
		final long timeout = sessionExpirationTimeout.toMillis();
		final long curtime = SessionManager.timems();
		sessions.forEach((id, session) -> {
			// Remove stale entries...
			if (curtime > session.getLastUpdate() + timeout) {
				log.info("Stale session found: " + id);
				idsToRemove.add(id);
				return;
			}

			// Remove expired sessions...
			if (session.hasExpirationTimestamp() && curtime > session.getExpirationTimestamp()) {
				log.info("Session '" + id + "' expired!");
				idsToRemove.add(id);
				return;
			}

			executor.execute(new SessionKeepAliveTask(session, log));
		});

		idsToRemove.forEach(this::remove);
	}

	/** Remove session */
	public void remove(String id)
	{
		final Session session = this.get(id);
		if (session == null)
			return;

		log.info("Removing session '" + id + "'...");
		session.onTimeout(endpoint, log);

		final var components = session.components()
				.keySet()
				.toArray(new String[0]);

		this.remove(id, components);
	}

	/** Remove session's components */
	public void remove(String sid, String... components)
	{
		this.remove(sid, Arrays.asList(components));
	}

	/** Remove session's components */
	public void remove(String sid, Collection<String> cids)
	{
		sessions.computeIfPresent(sid, (unused, session) -> {
			final var components = new ArrayList<SessionComponent>(cids.size());
			for (final var cid : cids) {
				final var component = session.components()
						.remove(cid);

				if (component != null)
					components.add(component);
			}

			if (components.size() > 0)
				log.info("Removing " + components.size() + " component(s) from session " + sid + "...");

			// Components should be removed in reverse-creation-order!
			components.sort((lhs, rhs) -> Long.compare(rhs.getCreationTime(), lhs.getCreationTime()));

			int numComponentsRemoved = 0;
			int numComponentsFailed = 0;

			for (final var component : components) {
				// Release component's resources
				final var cid = component.id();
				try {
					component.markAsRemoved();
					if (!component.isReleased() && !component.isEphemeral())
						endpoint.releaseComponent(cid);

					++numComponentsRemoved;
				}
				catch (Exception error) {
					log.log(Level.WARNING, "Releasing component " + cid + " from session " + sid + " failed!", error);
					++numComponentsFailed;
				}
			}

			if (numComponentsRemoved + numComponentsFailed > 0)
				log.info("Removed " + numComponentsRemoved + " component(s) from session " + sid + ", failed " + numComponentsFailed);

			if (session.components().isEmpty()) {
				// Session is empty and can be removed!
				log.info("Session '" + sid + "' removed");
				return null;
			}

			return session;
		});
	}

	public static long timems()
	{
		return System.currentTimeMillis();
	}


	// ========== Internal Helpers ====================

	@PostConstruct
	private void initialize()
	{
		final long delay = resourceExpirationTimeout.toMillis() / 2L;
		final Runnable trigger = () -> executor.execute(new RefreshTask());
		scheduler.scheduleAtFixedRate(trigger, delay, delay, TimeUnit.MILLISECONDS);
	}

	private class RefreshTask implements Runnable
	{
		@Override
		public void run()
		{
			final var manager = SessionManager.this;
			try {
				manager.update(executor);
			}
			catch (Exception error) {
				log.log(Level.WARNING, "Refreshing sessions failed!", error);
			}
		}
	}

	private class SessionKeepAliveTask implements Runnable
	{
		private final Session session;
		private final Logger log;

		public SessionKeepAliveTask(Session session, Logger log)
		{
			this.session = session;
			this.log = log;
		}

		@Override
		public void run()
		{
			session.keepalive(endpoint, log, session.isDetached());
		}
	}
}
