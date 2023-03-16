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

package com.openslx.eaas.common.databind;


public class DataUtils
{
	public static JsonDataUtils json()
	{
		return JSON;
	}

	public static XmlDataUtils xml()
	{
		return XML;
	}


	// ===== Internal Helpers ==============================

	private static final JsonDataUtils JSON = new JsonDataUtils();
	private static final XmlDataUtils XML = new XmlDataUtils();

	private DataUtils()
	{
		// Empty!
	}
}
