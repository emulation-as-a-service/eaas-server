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

import de.bwl.bwfla.common.utils.jaxb.JaxbType;
import de.bwl.bwfla.emil.Components;

import javax.ws.rs.NotFoundException;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;


@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public class Session extends JaxbType
{
	@XmlElement
	private final String id;

	@XmlElement
	private String name;

	private long expirationTimestamp = -1L;
	private long lifetime = -1L;
	private boolean detached = false;
	private boolean failed = false;
	private long lastUpdate;

	/** List of component IDs */
	private final Map<String, SessionComponent> components;


	public Session()
	{
		this(UUID.randomUUID().toString());
	}

	public Session(String id) {
		this.id = id;
		this.lastUpdate = SessionManager.timems();
		this.components = Collections.synchronizedMap(new HashMap<>());
	}

	public String id()
	{
		return id;
	}

	void setFailed()
	{
		failed = true;
	}

	public boolean hasExpirationTimestamp()
	{
		return expirationTimestamp > 0L;
	}

	long getExpirationTimestamp()
	{
		return expirationTimestamp;
	}

	public long getLifetime()
	{
		return lifetime;
	}

	public void setLifetime(long lifetime)
	{
		this.lifetime = lifetime;
		this.detached = lifetime >= 0L;
	}

	void setExpirationTimestamp(long timestamp)
	{
		this.expirationTimestamp = timestamp;
	}

	public boolean isDetached() {
		return detached;
	}

	public boolean isFailed() {
		return failed;
	}

	long getLastUpdate() {
		return lastUpdate;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName()
	{
		return name;
	}

	public Map<String, SessionComponent> components()
	{
		return components;
	}

	public SessionComponent component(String cid)
	{
		final var component = components.get(cid);
		if (component == null)
			throw new NotFoundException("Component '" + cid + "' was not found in network '" + id + "'!");

		return component;
	}

	public void onTimeout(Components endpoint, Logger log)
	{
		// Empty!
	}

	public void keepalive(Components endpoint, Logger log)
	{
		this.keepalive(endpoint, log, true);
	}

	public void keepalive(Components endpoint, Logger log, boolean refresh)
	{
		if (refresh)
			this.update();

		final Function<SessionComponent, Long> checker = (component) -> {
			try {
				endpoint.keepalive(component.id());
				return 0L;
			}
			catch (Exception error) {
				log.log(Level.WARNING, "Sending keepalive failed for component " + component.id() + "!");
				return 1L;
			}
		};

		final Optional<Long> numfailed = components.values()
				.stream()
				.filter((sc) -> !sc.isEphemeral())
				.map(checker)
				.reduce(Long::sum);

		if (numfailed.isPresent() && numfailed.get() > 0L)
			log.info(numfailed.get() + " out of " + components.size() + " component(s) failed in session " + id + "!");
	}

	private void update()
	{
		this.lastUpdate = SessionManager.timems();
	}
}