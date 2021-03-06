/************************************************************************************
*
*   This file is part of triki
*
*   Written by Donald McIntosh (dbm@opentechnology.net) 
*
*   triki is free software: you can redistribute it and/or modify
*   it under the terms of the GNU General Public License as published by
*   the Free Software Foundation, either version 3 of the License, or
*   (at your option) any later version.
*
*   triki is distributed in the hope that it will be useful,
*   but WITHOUT ANY WARRANTY; without even the implied warranty of
*   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
*   GNU General Public License for more details.
*
*   You should have received a copy of the GNU General Public License
*   along with triki.  If not, see <http://www.gnu.org/licenses/>.
*
************************************************************************************/

package net.opentechnology.triki.modules;

import net.opentechnology.triki.core.boot.StartupException;
import org.apache.wicket.ConverterLocator;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.settings.SecuritySettings;
import org.stringtemplate.v4.ST;

public interface Module {

	/**
	 * Initialise module
	 * @throws StartupException 
	 */
	void initMod() throws StartupException;

	void initWeb();

	public default void mountPages(WebApplication webApplication){

	}

	public default void addConverters(ConverterLocator defaultLocator){

	}

	public default void setAuthorisationStrategy(SecuritySettings securitySettings){

	}

	public default void addTemplateObjects(ST template, String url){

	}

}
