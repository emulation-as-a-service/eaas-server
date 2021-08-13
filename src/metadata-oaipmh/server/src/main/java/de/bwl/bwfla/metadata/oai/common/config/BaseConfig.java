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

package de.bwl.bwfla.metadata.oai.common.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.dspace.xoai.dataprovider.model.MetadataFormat;
import org.apache.tamaya.ConfigException;
import org.apache.tamaya.Configuration;
import org.apache.tamaya.ConfigurationProvider;

import javax.xml.transform.Transformer;


public abstract class BaseConfig
{
	public void load()
	{
		this.load(ConfigurationProvider.getConfiguration());
	}
	
	public abstract void load(Configuration config) throws ConfigException;


	@JsonIgnore
	public static String getMetaDataFormat()
	{
		return "eaasmd";
	}

	@JsonIgnore
	public static Transformer getMetaDataTransformer()
	{
		return MetadataFormat.identity();
	}
}
