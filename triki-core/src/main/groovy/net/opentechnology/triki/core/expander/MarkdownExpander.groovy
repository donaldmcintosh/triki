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

package net.opentechnology.triki.core.expander

import com.vladsch.flexmark.ast.Document
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.profiles.pegdown.Extensions
import com.vladsch.flexmark.profiles.pegdown.PegdownOptionsAdapter
import com.vladsch.flexmark.util.options.DataHolder
import net.opentechnology.triki.core.renderer.MarkdownRenderer
import net.opentechnology.triki.core.resources.ContentUtils
import net.opentechnology.triki.core.resources.ResourceException

import javax.inject.Inject

public class MarkdownExpander extends AbstractSourceExpander implements SourceExpander {
	
	@Inject
	private ContentUtils contentUtils;

	@Inject
	private MarkdownRenderer markdownRenderer
	
	public String expand(String url) throws ExpanderException{
		if(url.endsWith("md"))
		{
			try {
				URL fullUrl = new URL(url);
				String filename = fullUrl.getPath().replaceAll("^.*/", "");
				String markdown = contentUtils.getClasspathTextContent(filename);
				return expandString(markdown);
			}  catch (MalformedURLException e) {
				throw new ExpanderException("Bad url " + url);
			} catch (ResourceException e) {
				throw new ExpanderException("Could not get url " + url);
			}
		} else if(url.endsWith("html")){
			URL fullUrl = new URL(url);
			String filename = fullUrl.getPath().replaceAll("^.*/", "");
			String html = contentUtils.getClasspathTextContent(filename);
			return html;
		}
		else
		{
			throw new ExpanderException("Unexpected file type for URL " + url);
		}
	}

	@Override
	public String expandString(String markdown) throws ExpanderException {
		markdownRenderer.render(markdown)
	}

}
