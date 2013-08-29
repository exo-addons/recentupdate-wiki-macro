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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.wiki.mow.api.Wiki;
import org.exoplatform.wiki.mow.api.WikiNodeType;
import org.exoplatform.wiki.mow.core.api.wiki.PageImpl;
import org.exoplatform.wiki.rendering.builder.ReferenceBuilder;
import org.exoplatform.wiki.rendering.context.MarkupContextManager;
import org.exoplatform.wiki.service.WikiContext;
import org.exoplatform.wiki.service.WikiPageParams;
import org.exoplatform.wiki.service.WikiService;
import org.exoplatform.wiki.service.search.SearchResult;
import org.exoplatform.wiki.service.search.WikiSearchData;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.BulletedListBlock;
import org.xwiki.rendering.block.GroupBlock;
import org.xwiki.rendering.block.LinkBlock;
import org.xwiki.rendering.block.ListItemBlock;
import org.xwiki.rendering.block.SpaceBlock;
import org.xwiki.rendering.block.WordBlock;
import org.xwiki.rendering.listener.reference.DocumentResourceReference;
import org.xwiki.rendering.listener.reference.ResourceType;
import org.xwiki.rendering.macro.AbstractMacro;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.transformation.MacroTransformationContext;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Jul 22, 2013  
 */
@Component("recentlyupdated")
public class RecentlyUpdatedMacro extends AbstractMacro<RecentlyUpdatedMacroParameters>{

  private static final String NAME = "RecentlyUpdated";
  private static final String DESCRIPTION = "Lists wiki pages which were updated recently";
  
  private static final Log LOG = ExoLogger.getLogger(RecentlyUpdatedMacro.class.getName());
  
  @Inject
  private ComponentManager componentManager;
  
  @Inject
  private Execution execution;
  
  @Inject
  private MarkupContextManager markupContextManager;
  
  public RecentlyUpdatedMacro() {
    super(NAME, DESCRIPTION, RecentlyUpdatedMacroParameters.class);
  }

  @Override
  public List<Block> execute(RecentlyUpdatedMacroParameters parameters,
                             String content,
                             MacroTransformationContext context) throws MacroExecutionException {
    
    List<SearchResult> results = searchRecentlyUpdatedWikiPages(parameters);
    Block group = new GroupBlock();
    Block childrenBlock = new BulletedListBlock(Collections.<Block>emptyList());
    for (SearchResult res : results) {
      try {
        Block itemBlock = transformToBlock(res, context); 
        childrenBlock.addChild(itemBlock);
      } catch (Exception e) {
        if (LOG.isDebugEnabled()) {
          LOG.debug("Can not build result: " + res.getPath(), e);
        }
      }
    }
    group.addChild(childrenBlock);
    return Collections.singletonList((Block) group);
  }
  
  private List<SearchResult> searchRecentlyUpdatedWikiPages(RecentlyUpdatedMacroParameters params) 
      throws MacroExecutionException {
    WikiPageParams pageParams = getWikiContext();    
    //set constraints
     //wiki 
    WikiSearchData data = new WikiSearchData(null, null, pageParams.getType(), pageParams.getOwner());
    setWiki(data, params);
     //authors
    data.addPropertyConstraint(getAuthorConstraints(params.getAuthors()));
     //last modified date
    data.addPropertyConstraint(getTimeConstraint(params.getTime()));
     //contents
    data.setContent("*");
    //limit
    data.setLimit(getEntries(params.getEntries()));
    //search
    data.setSort("exo:lastModifiedDate");
    data.setOrder("DESC");
    WikiService wikiService = (WikiService) PortalContainer.getComponent(WikiService.class);
    try {
      return wikiService.search(data).getAll();
    } catch (Exception e) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Can not search recently updated wiki pages", e);
      }
      return new ArrayList<SearchResult>();
    }
  }
  
  private void setWiki(WikiSearchData data, RecentlyUpdatedMacroParameters params) {
    String wikiSt = params.getWikis();
    if (StringUtils.isNotBlank(wikiSt)) {
      WikiService wikiService = (WikiService) PortalContainer.getComponent(WikiService.class);
      if (!wikiSt.startsWith("/")) {
        wikiSt = "/" + wikiSt;
      }
      Wiki wiki = wikiService.getWikiById(params.getWikis());
      if (wiki != null) {
        data.setWikiType(wiki.getType());
        data.setWikiOwner(wiki.getOwner());
      }
    }
  }
  
  private String getAuthorConstraints(String authorList) {
    StringBuffer authorConstraint = new StringBuffer();
    authorConstraint.append(" AND ( ");
    if (StringUtils.isBlank(authorList)) {
      authorConstraint.append(WikiNodeType.Definition.EXO_LAST_MODIFIER)
                      .append(" IS NOT NULL");
    } else {
      int count = 0;
      for (String author : authorList.split(",")) {
        if (count++ > 0) {
          authorConstraint.append(" OR ");
        }
        authorConstraint.append(" ( ").append(WikiNodeType.Definition.EXO_LAST_MODIFIER).append("='").append(author).append("' )");
      }
    }
    authorConstraint.append(" ) ");  
    return authorConstraint.toString();
  }
  
  private String getTimeConstraint(String time) throws MacroExecutionException {
    StringBuffer timeConstraint = new StringBuffer();
    Calendar date = new GregorianCalendar();
    if (StringUtils.isNotBlank(time)) {
      timeConstraint.append(" AND ( ");
      String pattern = "((\\d)+d)?((\\d)+h)?((\\d)+m)?((\\d)+s)?";
      if (time.matches(pattern)) {
        //get time value
        int day = getValue(time, "(\\d)+d");
        int hour = getValue(time, "(\\d)+h");
        int minute = getValue(time, "(\\d)+m");
        int second = getValue(time, "(\\d)+s");
        date.add(Calendar.DATE, (-1)*day);
        date.add(Calendar.HOUR, (-1)*hour);
        date.add(Calendar.MINUTE, (-1)*minute);
        date.add(Calendar.SECOND, (-1)*second);
        //append constraint
        SimpleDateFormat formatDateTime = new SimpleDateFormat();
        formatDateTime.applyPattern("YYYY-MM-DDThh:mm:ss.sTZD");
        String calculatedTime = formatDateTime.format(date.getTime()) + "T00:00:00.000";
        timeConstraint.append(WikiNodeType.Definition.EXO_LAST_MODIFIED_DATE)
                      .append(" >= TIMESTAMP '")
                      .append(calculatedTime)
                      .append("'");
      } else {
        throw new MacroExecutionException(String.format("Time is not correct: %s, please use pattern d h m s", 
                                                        time));
      }
      timeConstraint.append(" ) ");
    }
    return timeConstraint.toString();
  }
  
  private int getValue(String st, String pattern) {
    Pattern pt = Pattern.compile(pattern);
    Matcher matcher = pt.matcher(st);
    if (!matcher.find()) {
      return 0;
    }
    String value = matcher.group();
    return Integer.parseInt(value.substring(0, value.length() - 1));
  }
  
  private int getEntries(String entries) throws MacroExecutionException {
    if (StringUtils.isBlank(entries)) {
      return 10;
    }
    try {
      return Integer.parseInt(entries);
    } catch(NumberFormatException e) {
      throw new MacroExecutionException("entries value is incorrect", e);
    }
  }

  private WikiContext getWikiContext() {
    ExecutionContext ec = execution.getContext();
    if (ec != null) {
      WikiContext wikiContext = (WikiContext) ec.getProperty(WikiContext.WIKICONTEXT);
      return wikiContext;
    }
    return null;
  }
  
  private ListItemBlock transformToBlock(SearchResult res, MacroTransformationContext context) throws Exception {
    List<Block> blocks = new ArrayList<Block>();
    String pageName = res.getPageName();
    if (pageName.indexOf("/") >= 0) {
      pageName = pageName.substring(pageName.lastIndexOf("/") + 1);
    }
    WikiPageParams params = markupContextManager.getMarkupContext(pageName, ResourceType.DOCUMENT);
    DocumentResourceReference link = new DocumentResourceReference(getReferenceBuilder(context).build(params));
    //link to wiki page
    List<Block> content = new ArrayList<Block>();
    content.add(new WordBlock(res.getTitle()));
    LinkBlock linkBlock = new LinkBlock(content, link, true);
    blocks.add(linkBlock);
    //created by or updated by
    blocks.add(new SpaceBlock());
    WikiService wikiService = (WikiService)ExoContainerContext.getCurrentContainer().
                               getComponentInstanceOfType(WikiService.class); 
    PageImpl page = (PageImpl) wikiService.getPageById(params.getType(), params.getOwner(), params.getPageId());
    String modifyText = page.getCreatedDate().equals(page.getUpdatedDate()) ?
                        "created by " : " updated by";
    blocks.add(new WordBlock(modifyText));
    //user name
    blocks.add(new SpaceBlock());
    blocks.add(new WordBlock(page.getAuthor()));
    //time
    blocks.add(new SpaceBlock());
    SimpleDateFormat formatDateTime = new SimpleDateFormat();
    formatDateTime.applyPattern("yyyy.MMMM.dd GGG hh:mm aaa");
    blocks.add(new WordBlock(formatDateTime.format(
                             page.getJCRPageNode().getProperty(WikiNodeType.Definition.UPDATED_DATE).getDate().getTime())));
    
    return new ListItemBlock(blocks);
  }
  
  private ReferenceBuilder getReferenceBuilder(MacroTransformationContext context) throws MacroExecutionException {
    try {
      return componentManager.getInstance(ReferenceBuilder.class, context.getSyntax().toIdString());
    } catch (ComponentLookupException e) {
      throw new MacroExecutionException(String.format("Failed to find reference builder for syntax %s", context.getSyntax().toIdString()), e);
    }
  }
  
  @Override
  public boolean supportsInlineMode() {
    return true;
  }
}

