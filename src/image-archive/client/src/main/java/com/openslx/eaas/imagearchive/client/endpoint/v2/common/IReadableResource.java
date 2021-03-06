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

package com.openslx.eaas.imagearchive.client.endpoint.v2.common;

import com.openslx.eaas.imagearchive.api.v2.common.IReadable;
import com.openslx.eaas.imagearchive.api.v2.common.ResolveOptionsV2;
import de.bwl.bwfla.common.exceptions.BWFLAException;

import java.util.function.Function;


public interface IReadableResource<T>
{
	// ===== IReadable API ==============================

	default String resolve(String id) throws BWFLAException
	{
		return this.resolve(id, null);
	}

	default String resolve(String id, ResolveOptionsV2 options) throws BWFLAException
	{
		return this.api()
				.resolve(id, options);
	}

	default T fetch(String id) throws BWFLAException
	{
		return this.api()
				.fetch(id);
	}

	default <U> U fetch(String id, Function<T,U> mapper) throws BWFLAException
	{
		return mapper.apply(this.fetch(id));
	}


	// ===== Internal Helpers ==============================

	IReadable<T> api();
}
