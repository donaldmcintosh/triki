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

package net.opentechnology.triki.core.template;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

import java.io.FileNotFoundException;

import javax.servlet.http.HttpSession;

import net.opentechnology.triki.core.boot.TrikiBaseTest;
import net.opentechnology.triki.core.dto.PropertyDto;
import net.opentechnology.triki.core.expander.ExpanderException;
import net.opentechnology.triki.core.expander.SourceExpander;
import net.opentechnology.triki.core.renderer.DateRenderer;

import net.opentechnology.triki.core.renderer.MarkdownRenderer;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.STGroupString;

import org.apache.jena.datatypes.xsd.XSDDateTime;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;

@RunWith(MockitoJUnitRunner.class)
public class ResourceAdaptorTest extends TrikiBaseTest {
	
	@Mock private SourceExpander expander;
	@Mock private Resource person;
	@Mock private MarkdownRenderer markdownRenderer;
	
	private Logger logger = Logger.getLogger(this.getClass());
	
	@Before
	public void setup() throws FileNotFoundException {
		loadModel();
	}
	
	public String render(String url, String templateDef) throws TemplateException {
		ResourceAdaptor ra = new ResourceAdaptor(model);
		ra.setExpander(expander);
		ra.setAuthManager(authMgr);
		ra.setMarkdownRenderer(new MarkdownRenderer());
		STGroup g = new STGroupString("", templateDef, '$', '$');
		g.registerModelAdaptor(String.class, ra);
		g.registerModelAdaptor(RDFNode.class, ra);
		g.registerRenderer(XSDDateTime.class, new DateRenderer());
		ST st = g.getInstanceOf("foo");
		st.add("resource", url);

		return st.render();
	}
	
	@Test
	public void testObjectDescription() throws TemplateException{
		when(authMgr.allowAccess(Matchers.anyString())).thenReturn(true);
		when(session.getAttribute(Matchers.anyString())).thenReturn(null);
		String resource = "http://www.donaldmcintosh.net/resource/donald+mcintosh";
		String templateDef = "foo(resource) ::= \"$resource.dcterms_description$ is a $resource.rdf_type/dcterms_description$\"";
		String result = render(resource, templateDef);
		assertTrue("Got result " + result, result.equals("Donald McIntosh\n is a Person"));
	}

	@Test
	public void testLiteral() throws TemplateException{
		when(authMgr.allowAccess(Matchers.anyString())).thenReturn(true);
		String resource = "http://www.donaldmcintosh.net/resource/noah+mcintosh";
		String templateDef = "foo(resource) ::= \"He is called $resource.dcterms_description$\"";
		String result = render(resource, templateDef);
		assertTrue(result.equals("He is called Noah McIntosh\n"));
	}
	
	@Test
	public void testLikesAsString() throws TemplateException{
		when(authMgr.allowAccess(Matchers.anyString())).thenReturn(true);
		String resource = "http://www.donaldmcintosh.net/resource/donald+mcintosh";
		String templateDef = "foo(resource) ::= \"$resource.dcterms_description$ likes $resource.dcterms_likes/dcterms_description$\"";
		String result = render(resource, templateDef);
		assertTrue("Got " + result, result.equals("Donald McIntosh\n likes Daniel McIntoshNoah McIntosh"));
	}
	
	@Test
	public void testWithUrl() throws TemplateException{
		when(authMgr.allowAccess(Matchers.anyString())).thenReturn(true);
		String resource = "http://www.donaldmcintosh.net/resource/donald+mcintosh";
		String templateDef = "foo(resource) ::= \"$resource.dcterms_description$ is a <a href='$resource.rdf_type$' > $resource.rdf_type/dcterms_description$ </a>\"";
		String result = render(resource, templateDef);
		assertTrue("Got " + result, result.equals("Donald McIntosh\n is a <a href='http://www.foaf.org/0.1/Person' > Person </a>"));
	}
	
	@Test
	public void testLikesIterate() throws TemplateException{
		when(authMgr.allowAccess(Matchers.anyString())).thenReturn(true);
		String resource = "http://www.donaldmcintosh.net/resource/donald+mcintosh";
		String templateDef = "foo(resource) ::= \"$resource.dcterms_description$ likes $resource.dcterms_likes/dcterms_description:{ x | $x$ }$\"";
		String result = render(resource, templateDef);
		assertTrue("Got " + result, result.equals("Donald McIntosh\n likes Daniel McIntosh Noah McIntosh "));
	}

	@Test
	public void testLikesAndLinksIterate() throws TemplateException{
		when(authMgr.allowAccess(Matchers.anyString())).thenReturn(true);
		String resource = "http://www.donaldmcintosh.net/resource/donald+mcintosh";
		String templateDef = "foo(resource) ::= \"$resource.dcterms_description$ likes $resource.dcterms_likes:{ likes | <a href='$likes.relurl$'>$likes.dcterms_description$</a> }$\"";
		String result = render(resource, templateDef);
		assertTrue("Got " + result, result.equals("Donald McIntosh\n likes <a href='http://www.donaldmcintosh.net/resource/daniel+mcintosh'>Daniel McIntosh\n</a> <a href='http://www.donaldmcintosh.net/resource/noah+mcintosh'>Noah McIntosh\n</a> "));
	}
	
	@Test
	public void testBackReference() throws TemplateException{
		when(authMgr.allowAccess(Matchers.anyString())).thenReturn(true);
		String resource = "http://www.donaldmcintosh.net/resource/IMG_0022.IMG";
		String templateDef = "foo(resource) ::= \"Image $resource.dcterms_description$ is in albums $resource.Striki_contains/dcterms_description:{ x | $x$ }$\"";
		String result = render(resource, templateDef);
		assertTrue("Got " + result, result.equals("Image Cala Llenya photo\n is in albums August 2014 Album Ibiza 2014 Album "));
	}
	
	@Test
	public void testBackReferenceWhatICreated() throws TemplateException{
		when(authMgr.allowAccess(Matchers.anyString())).thenReturn(true);
		String resource = "http://www.donaldmcintosh.net/resource/donald+mcintosh";
		String templateDef = "foo(resource) ::= \"All that I have created: $resource.Sdcterms_creator:{ x | $x.dcterms_description$, }$\"";
		String result = render(resource, templateDef);
		assertTrue("Got \"" + result + "\"", result.equals("All that I have created: A fascinating blog\n, Ibiza 2014 Album\n, "));
	}
	
	@Test
	public void testMarkdownExpander() throws TemplateException, ExpanderException{
		when(authMgr.allowAccess(Matchers.anyString())).thenReturn(true);
		String resource = "http://www.donaldmcintosh.net/resource/blog1";
		when(expander.expand("http://www.donaldmcintosh.net/resource/whatever")).thenReturn("hello!");
		String templateDef = "foo(resource) ::= \"Include this $resource.triki_include$\"";
		String result = render(resource, templateDef);
		assertTrue("Got " + result, result.equals("Include this hello!"));
	}

	@Test
	public void testHtmlExpander() throws TemplateException, ExpanderException{
		when(authMgr.allowAccess(Matchers.anyString())).thenReturn(true);
		String resource = "http://www.donaldmcintosh.net/resource/blog1";
		when(expander.expand("http://www.donaldmcintosh.net/resource/whatever")).thenReturn("<p><div id='main'>&#163;hello!</div><p>");
		String templateDef = "foo(resource) ::= \"Include this $resource.triki_include$\"";
		String result = render(resource, templateDef);
		assertTrue("Got " + result, result.equals("Include this <p><div id='main'>&#163;hello!</div><p>"));
	}
	
	@Test
	public void testSparql() throws TemplateException{
		when(authMgr.allowAccess(Matchers.anyString())).thenReturn(true);
		String resource = "http://www.donaldmcintosh.net/resource/recentblogs";
		String templateDef = "foo(resource) ::= \"Recent blogs: $resource.triki_sparql:{ x | $x.dcterms_description$, }$\"";
		String result = render(resource, templateDef);
		assertTrue("Got " + result, result.equals("Recent blogs: Eivissa holiday\n, Up North holiday\n, Berliner holiday\n, "));
	}
	
	@Test
	public void testLinkThruSparql() throws TemplateException{
		when(authMgr.allowAccess(Matchers.anyString())).thenReturn(true);
		String resource = "http://www.donaldmcintosh.net/resource/home";
		String templateDef = "foo(resource) ::= \"Recent blogs: $resource.resource_blogsummary:{ recentblog | $recentblog.triki_sparql:{ blog | $blog.dcterms_description$, }$}$\"";
		String result = render(resource, templateDef);
		assertTrue("Got " + result, result.equals("Recent blogs: Eivissa holiday\n, Up North holiday\n, Berliner holiday\n, "));
	}
	
   @Test
    public void testOrderedBlogs() throws TemplateException{
	   when(authMgr.allowAccess(Matchers.anyString())).thenReturn(true);
        String resource = "http://www.donaldmcintosh.net/resource/blog";
        String templateDef = "foo(resource) ::= \"Recent blogs: $resource.Srdf_type:{ blog | $blog.dcterms_description$, }$\"";
        String result = render(resource, templateDef);
	    assertTrue("Got " + result, result.equals("Recent blogs: Eivissa holiday\n, Up North holiday\n, Berliner holiday\n, New York holiday\n, France holiday\n, "));
    }

	@Test
	public void testOrderedModules() throws TemplateException{
		when(authMgr.allowAccess(Matchers.anyString())).thenReturn(true);
		String resource = "http://www.donaldmcintosh.net/resource/module";
		String templateDef = "foo(resource) ::= \"Modules: $resource.Srdf_type:{ module | $module.dcterms_description$, }$\"";
		String result = render(resource, templateDef);
		assertTrue("Got " + result, result.equals("Modules: Basics\n, Introduction\n, Environment\n, "));
	}

	@Test
	public void testOrderedModulesLinkedToPage() throws TemplateException{
		when(authMgr.allowAccess(Matchers.anyString())).thenReturn(true);
		String resource = "http://www.donaldmcintosh.net/resource/modules";
		String templateDef = "foo(resource) ::= \"Modules: $resource.property_module:{ module | $module.dcterms_description$, }$\"";
		String result = render(resource, templateDef);
		assertTrue("Got " + result, result.equals("Modules: Environment\n, Introduction\n, Basics\n, "));
	}
   
   @Test
   public void testReverseOrderedBlogs() throws TemplateException{
	   when(authMgr.allowAccess(Matchers.anyString())).thenReturn(true);
       String resource = "http://www.donaldmcintosh.net/resource/blog";
       String templateDef = "foo(resource) ::= \"Recent blogs: $reverse(resource.Srdf_type:{ blog | $blog.dcterms_description$, })$\"";
       String result = render(resource, templateDef);
	   assertTrue("Got " + result, result.equals("Recent blogs: France holiday\n, New York holiday\n, Berliner holiday\n, Up North holiday\n, Eivissa holiday\n, "));
   }

   @Test
   public void testDateRenderer() throws TemplateException{
	   when(authMgr.allowAccess(Matchers.anyString())).thenReturn(true);
       when(session.getAttribute(Matchers.anyString())).thenReturn(null);
       String resource = "http://www.donaldmcintosh.net/resource/holiday-aboyne";
       String templateDef = "foo(resource) ::= \"Resource was created at $resource.dcterms_created$\"";
       String result = render(resource, templateDef);
       assertEquals(result, "Resource was created at Sat 22 Mar, 2014, 19:00");
   }

	@Test
	public void testObjectDescriptionWithUTF8() throws TemplateException{
		when(authMgr.allowAccess(Matchers.anyString())).thenReturn(true);
		when(session.getAttribute(Matchers.anyString())).thenReturn(null);
		String resource = "http://www.donaldmcintosh.net/resource/holiday-aboyne";
		String templateDef = "foo(resource) ::= \"$resource.dcterms_weather$\"";
		String result = render(resource, templateDef);
		assertEquals(result, "Monday: Sunny Intervals, Maximum Temperature: 8\u00B0C (46\u00B0F)");
	}

	@Test
	public void testInlineRenderer() throws TemplateException, ExpanderException{
		when(authMgr.allowAccess(Matchers.anyString())).thenReturn(true);
		String resource = "http://www.donaldmcintosh.net/resource/note1";
		String templateDef = "foo(resource) ::= \"Include this $resource.triki_webcontent$\"";
		String result = render(resource, templateDef);
		assertEquals(result, "Include this <a href=\"http://www.donaldmcintosh.net/hello\">hello</a>\n");
	}
	
}
