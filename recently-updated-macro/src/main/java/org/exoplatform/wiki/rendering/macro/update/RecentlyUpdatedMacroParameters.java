/*
 * Copyright (C) 2003-2013 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.wiki.rendering.macro.update;

import org.apache.commons.lang.StringUtils;
import org.xwiki.properties.annotation.PropertyDescription;
/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Jul 22, 2013  
 */
public class RecentlyUpdatedMacroParameters {
  
  private String entries = StringUtils.EMPTY;
  private String wikis = StringUtils.EMPTY;
  private String authors = StringUtils.EMPTY;
  private String time = StringUtils.EMPTY;
  
  /**
   * gets number of entry to display
   * @return the entry number
   */
  public String getEntries() {
    return entries;
  }

  /**
   * @param entries the entry number
   */
  @PropertyDescription("Number of entry to display in macro")
  public void setEntries(String entries) {
    this.entries = entries;
  }
  
  /**
   * gets the wikis in which pages are updated
   * @return the wiki list
   */
  public String getWikis() {
    return wikis;
  }
  
  /**
   * @param wikis the wikis
   */
  @PropertyDescription("Wikis in which pages are updated")
  public void setWikis(String wikis) {
    this.wikis = wikis;
  }

  /**
   * gets the authors who recently modified the wiki pages 
   * @return the authors
   */
  public String getAuthors() {
    return authors;
  }
  
  /**
   * @param authors the authors
   */
  @PropertyDescription("Users who recently modified the wiki pages")
  public void setAuthors(String authors) {
    this.authors = authors;
  }
  
  /**
   * gets the time interval in which pages are modified 
   * @return the time interval;
   */
  public String getTime() {
    return time;
  }
  
  /**
   * @param time the time
   */
  @PropertyDescription("Time interval in which pages are modified")
  public void setTime(String time) {
    this.time = time;
  }
 
}
