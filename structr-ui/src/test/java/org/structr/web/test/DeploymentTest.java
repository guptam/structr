/**
 * Copyright (C) 2010-2016 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.web.test;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import org.junit.Test;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;
import org.structr.web.common.StructrUiTest;
import org.structr.web.entity.dom.Content;
import org.structr.web.entity.dom.DOMElement;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Page;
import org.structr.web.entity.dom.ShadowDocument;
import org.structr.web.entity.dom.Template;
import org.structr.web.entity.html.Body;
import org.structr.web.entity.html.Div;
import org.structr.web.entity.html.Head;
import org.structr.web.entity.html.Html;
import org.structr.web.entity.html.Link;
import org.structr.web.entity.html.Script;
import org.structr.web.entity.html.Table;
import org.structr.web.entity.html.Tbody;
import org.structr.web.maintenance.DeployCommand;
import org.structr.websocket.command.CloneComponentCommand;
import org.structr.websocket.command.CreateComponentCommand;
import org.w3c.dom.Node;

public class DeploymentTest extends StructrUiTest {

	@Test
	public void test01SimplePage() {

		// setup
		try (final Tx tx = app.tx()) {

			Page.createSimplePage(securityContext, "test01");

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		// test
		compare(calculateHash(), true);
	}

	@Test
	public void test02Visibilities() {

		// setup
		try (final Tx tx = app.tx()) {

			final Page page       = Page.createNewPage(securityContext,   "test02");
			final Html html       = createElement(page, page, "html");
			final Head head       = createElement(page, html, "head");
			createElement(page, head, "title", "test02");

			final Body body       = createElement(page, html, "body");

			// create a div for admin only
			{
				final Div div1 = createElement(page, body, "div");
				createElement(page, div1, "h1", "private - ${find('User')}");

				div1.setProperty(DOMNode.showConditions, "me.isAdmin");
			}

			// create a private div
			{
				final Div div1 = createElement(page, body, "div");
				 createElement(page, div1, "h1", "private - test abcdefghjiklmnopqrstuvwyzöäüßABCDEFGHIJKLMNOPQRSTUVWXYZÖÄÜ?\"'");

				div1.setProperty(DOMNode.showConditions, "me.isAdmin");
			}

			// create a protected div
			{
				final Div div1 = createElement(page, body, "div");
				createElement(page, div1, "h1", "protected - $%&/()=?¼½¬{[]}");

				div1.setProperty(DOMNode.visibleToPublicUsers,        false);
				div1.setProperty(DOMNode.visibleToAuthenticatedUsers,  true);
			}

			// create a public div
			{
				final Div div1 = createElement(page, body, "div");
				createElement(page, div1, "h1", "public");

				div1.setProperty(DOMNode.visibleToPublicUsers,        true);
				div1.setProperty(DOMNode.visibleToAuthenticatedUsers, true);
			}

			// create a public only div
			{
				final Div div1 = createElement(page, body, "div");
				createElement(page, div1, "h1", "public only");

				div1.setProperty(DOMNode.visibleToPublicUsers,         true);
				div1.setProperty(DOMNode.visibleToAuthenticatedUsers, false);
			}

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		// test
		compare(calculateHash(), true);
	}

	@Test
	public void test03ContentTypes() {

		// setup
		try (final Tx tx = app.tx()) {

			final Page page = Page.createNewPage(securityContext,   "test03");
			final Html html = createElement(page, page, "html");
			final Head head = createElement(page, html, "head");
			createElement(page, head, "title", "test03");

			final Body body       = createElement(page, html, "body");
			final Div div1        = createElement(page, body, "div");
			final Script script   = createElement(page, div1, "script");
			final Content content = createContent(page, script,
				"$(function () {\n\n" +
				"$('a[data-toggle=\"tab\"]').on('click', function (e) {\n\n" +
				"var id = $(e.target).attr(\"href\").substr(1) // activated tab\n" +
				"window.location.hash = id;\n" +
				"});\n\n" +
				"});"
			);

			// workaround for strange importer behaviour
			script.setProperty(Script._type, "text/javascript");
			content.setProperty(Content.contentType, "text/javascript");

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		// test
		compare(calculateHash(), true);
	}

	@Test
	public void test04ContentTypes() {

		// setup
		try (final Tx tx = app.tx()) {

			final Page page = Page.createNewPage(securityContext,   "test04");
			final Html html = createElement(page, page, "html");
			final Head head = createElement(page, html, "head");
			createElement(page, head, "title", "test04");
			createElement(page, head, "link");
			createElement(page, head, "link");
			createComment(page, head, "commentöäüÖÄÜß+#");

			final Link link3  = createElement(page, head, "link");

			link3.setProperty(Link._href, "/");
			link3.setProperty(Link._media, "screen");
			link3.setProperty(Link._type, "stylesheet");

			final Body body       = createElement(page, html, "body");
			final Div div1        = createElement(page, body, "div");
			createElement(page, div1, "h1", "private");

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		// test
		compare(calculateHash(), true);
	}

	@Test
	public void test05SimpleTemplateInPage() {

		// setup
		try (final Tx tx = app.tx()) {

			final Page page = Page.createNewPage(securityContext,   "test05");
			final Html html = createElement(page, page, "html");
			final Head head = createElement(page, html, "head");
			createElement(page, head, "title", "test05");

			final Body body = createElement(page, html, "body");
			final Div div1  = createElement(page, body, "div");

			final Template template = createTemplate(page, div1, "template source - öäüÖÄÜß'\"'`");

			template.setProperty(Template.functionQuery, "find('User')");
			template.setProperty(Template.dataKey, "user");

			// append children to template object
			createElement(page, template, "div");
			createElement(page, template, "div");

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		// test
		compare(calculateHash(), true);
	}

	@Test
	public void test06SimpleTemplateInSharedComponents() {

		// setup
		try (final Tx tx = app.tx()) {

			final Page page = Page.createNewPage(securityContext,   "test06");
			final Html html = createElement(page, page, "html");
			final Head head = createElement(page, html, "head");
			createElement(page, head, "title", "test06");

			final Body body = createElement(page, html, "body");
			createElement(page, body, "div");

			final ShadowDocument shadowDocument = CreateComponentCommand.getOrCreateHiddenDocument();
			createTemplate(shadowDocument, null, "template source - öäüÖÄÜß'\"'`");

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		// test
		compare(calculateHash(), true);
	}

	@Test
	public void test07SimpleSharedTemplate() {

		// setup
		try (final Tx tx = app.tx()) {

			final Page page = Page.createNewPage(securityContext,   "test07");
			final Html html = createElement(page, page, "html");
			final Head head = createElement(page, html, "head");
			createElement(page, head, "title", "test07");

			final Body body = createElement(page, html, "body");
			final Div div1  = createElement(page, body, "div");

			final Template template = createTemplate(page, div1, "template source - öäüÖÄÜß'\"'`");

			createComponent(template);

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		// test
		compare(calculateHash(), true);
	}

	@Test
	public void test08SharedTemplateInTwoPages() {

		// setup
		try (final Tx tx = app.tx()) {

			// create first page
			final Page page1 = Page.createNewPage(securityContext,   "test08_1");
			final Html html1 = createElement(page1, page1, "html");
			final Head head1 = createElement(page1, html1, "head");
			createElement(page1, head1, "title", "test08_1");

			final Body body1 = createElement(page1, html1, "body");
			final Div div1   = createElement(page1, body1, "div");

			final Template template1 = createTemplate(page1, div1, "template source - öäüÖÄÜß'\"'`");
			final Template component = createComponent(template1);


			// create second page
			final Page page2 = Page.createNewPage(securityContext,   "test08_2");
			final Html html2 = createElement(page2, page2, "html");
			final Head head2 = createElement(page2, html2, "head");
			createElement(page2, head2, "title", "test08_2");

			final Body body2 = createElement(page2, html2, "body");
			final Div div2   = createElement(page2, body2, "div");

			// re-use template from above
			cloneComponent(component, div2);

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		// test
		compare(calculateHash(), true);
	}

	@Test
	public void test08SharedTemplatesWithChildren() {

		// setup
		try (final Tx tx = app.tx()) {

			// create first page
			final Page page1 = Page.createNewPage(securityContext,   "test08_1");
			final Html html1 = createElement(page1, page1, "html");
			final Head head1 = createElement(page1, html1, "head");
			createElement(page1, head1, "title", "test08_1");

			final Body body1 = createElement(page1, html1, "body");
			final Div div1   = createElement(page1, body1, "div");

			final Template template1 = createTemplate(page1, div1, "template source - öäüÖÄÜß'\"'`");
			createElement(page1, template1, "div", "test1");
			createElement(page1, template1, "div", "test1");

			final Template component = createComponent(template1);

			// create second page
			final Page page2 = Page.createNewPage(securityContext,   "test08_2");
			final Html html2 = createElement(page2, page2, "html");
			final Head head2 = createElement(page2, html2, "head");
			createElement(page2, head2, "title", "test08_2");

			final Body body2 = createElement(page2, html2, "body");
			final Div div2   = createElement(page2, body2, "div");

			// re-use template from above
			cloneComponent(component, div2);

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		// test
		compare(calculateHash(), true);
	}

	@Test
	public void test09SharedComponent() {

		// setup
		try (final Tx tx = app.tx()) {

			// create first page
			final Page page1 = Page.createNewPage(securityContext,   "test09_1");
			final Html html1 = createElement(page1, page1, "html");
			final Head head1 = createElement(page1, html1, "head");
			createElement(page1, head1, "title", "test09_1");

			final Body body1 = createElement(page1, html1, "body");
			final Div div1   = createElement(page1, body1, "div");

			createElement(page1, div1, "div", "test1");
			createElement(page1, div1, "div", "test1");

			final Div component = createComponent(div1);

			// create second page
			final Page page2 = Page.createNewPage(securityContext,   "test09_2");
			final Html html2 = createElement(page2, page2, "html");
			final Head head2 = createElement(page2, html2, "head");
			createElement(page2, head2, "title", "test09_2");

			final Body body2 = createElement(page2, html2, "body");
			final Div div2   = createElement(page2, body2, "div");

			// re-use template from above
			cloneComponent(component, div2);

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		// test
		compare(calculateHash(), true);
	}

	@Test
	public void test10TemplateInTbody() {

		// setup
		try (final Tx tx = app.tx()) {

			// create first page
			final Page page1 = Page.createNewPage(securityContext,   "test10");
			final Html html1 = createElement(page1, page1, "html");
			final Head head1 = createElement(page1, html1, "head");
			createElement(page1, head1, "title", "test09_1");

			final Body body1  = createElement(page1, html1, "body");
			final Table table = createElement(page1, body1, "table");
			final Tbody tbody = createElement(page1, table, "tbody");

			final Template template1 = createTemplate(page1, tbody, "<tr><td>${user.name}</td></tr>");

			template1.setProperty(DOMNode.functionQuery, "find('User')");
			template1.setProperty(DOMNode.dataKey, "user");

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		// test
		compare(calculateHash(), true);
	}

	// ----- private methods -----
	private void compare(final String sourceHash, final boolean deleteTestDirectory) {

		doImportExportRoundtrip(deleteTestDirectory);

		assertEquals("Invalid deployment roundtrip result", sourceHash, calculateHash());
	}


	private void doImportExportRoundtrip(final boolean deleteTestDirectory) {

		final DeployCommand cmd = app.command(DeployCommand.class);
		final Path tmp          = Paths.get("/tmp/structr-deployment-test" + System.currentTimeMillis() + System.nanoTime());

		try {
			if (tmp != null) {

				// export to temp directory
				final Map<String, Object> firstExportParams = new HashMap<>();
				firstExportParams.put("mode", "export");
				firstExportParams.put("target", tmp.toString());

				// execute deploy command
				cmd.execute(firstExportParams);

				// clean database
				try (final Tx tx = app.tx()) {

					for (final DOMNode domNode : app.nodeQuery(DOMNode.class).getAsList()) {
						app.delete(domNode);
					}

					tx.success();

				} catch (FrameworkException fex) {
					fail("Unexpected exception.");
				}

				// import from exported source
				final Map<String, Object> firstImportParams = new HashMap<>();
				firstImportParams.put("source", tmp.toString());

				// execute deploy command
				cmd.execute(firstImportParams);

			} else {

				fail("Unable to create temporary directory.");
			}

		} catch (FrameworkException fex) {

			fex.printStackTrace();

		} finally {

			if (deleteTestDirectory) {

				try {
					// clean directories
					Files.walkFileTree(tmp, new DeletingFileVisitor());
					Files.delete(tmp);

				} catch (IOException ioex) {}
			}
		}
	}

	private String calculateHash() {

		final StringBuilder buf = new StringBuilder();

		try (final Tx tx = app.tx()) {

			for (final Page page : app.nodeQuery(Page.class).sort(AbstractNode.name).getAsList()) {

				System.out.print("############################# ");

				calculateHash(page, buf, 0);
			}

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception");
		}

		return buf.toString();//DigestUtils.md5Hex(buf.toString());
	}

	private void calculateHash(final DOMNode start, final StringBuilder buf, final int depth) {

		buf.append(start.getType()).append("{");

		hash(start, buf);

		// indent
		for (int i=0; i<depth; i++) {
			System.out.print("    ");
		}

		System.out.println(start.getType());

		if (start instanceof ShadowDocument) {

			for (final DOMNode child : ((ShadowDocument)start).getProperty(Page.elements)) {

				// only include toplevel elements of the shadow document
				if (child.getProperty(DOMNode.parent) == null) {

					calculateHash(child, buf, depth+1);
				}
			}

		} else {

			for (final DOMNode child : start.getProperty(DOMNode.children)) {

				calculateHash(child, buf, depth+1);
			}
		}

		buf.append("}");
	}

	private void hash(final DOMNode node, final StringBuilder buf) {

		// AbstractNode
		buf.append(valueOrEmpty(node, AbstractNode.name));
		buf.append(valueOrEmpty(node, AbstractNode.visibleToPublicUsers));
		buf.append(valueOrEmpty(node, AbstractNode.visibleToAuthenticatedUsers));

		// DOMNode
		buf.append(valueOrEmpty(node, DOMNode.showConditions));
		buf.append(valueOrEmpty(node, DOMNode.hideConditions));
		buf.append(valueOrEmpty(node, DOMNode.showForLocales));
		buf.append(valueOrEmpty(node, DOMNode.hideForLocales));
		buf.append(valueOrEmpty(node, DOMNode.hideOnIndex));
		buf.append(valueOrEmpty(node, DOMNode.hideOnDetail));
		buf.append(valueOrEmpty(node, DOMNode.renderDetails));

		final Page ownerDocument = node.getProperty(DOMNode.ownerDocument);
		if (ownerDocument != null) {

			buf.append(valueOrEmpty(ownerDocument, AbstractNode.name));
		}

		// DOMElement
		buf.append(valueOrEmpty(node, DOMElement.dataKey));
		buf.append(valueOrEmpty(node, DOMElement.restQuery));
		buf.append(valueOrEmpty(node, DOMElement.cypherQuery));
		buf.append(valueOrEmpty(node, DOMElement.xpathQuery));
		buf.append(valueOrEmpty(node, DOMElement.functionQuery));

		// Content
		buf.append(valueOrEmpty(node, Content.contentType));
		buf.append(valueOrEmpty(node, Content.content));

		// Page
		buf.append(valueOrEmpty(node, Page.cacheForSeconds));
		buf.append(valueOrEmpty(node, Page.dontCache));
		buf.append(valueOrEmpty(node, Page.pageCreatesRawData));
		buf.append(valueOrEmpty(node, Page.position));
		buf.append(valueOrEmpty(node, Page.showOnErrorCodes));

		// HTML attributes
		if (node instanceof DOMElement) {

			for (final PropertyKey key : ((DOMElement)node).getHtmlAttributes()) {

				buf.append(valueOrEmpty(node, key));
			}
		}
	}

	private String valueOrEmpty(final GraphObject obj, final PropertyKey key) {

		final Object value = obj.getProperty(key);
		if (value != null) {

			return key.jsonName() + "=" + value.toString() + ";";
		}

		return "";
	}

	private <T extends Node> T createElement(final Page page, final DOMNode parent, final String tag, final String... content) {

		final T child = (T)page.createElement(tag);
		parent.appendChild((DOMNode)child);

		if (content != null && content.length > 0) {

			for (final String text : content) {

				final Node node = page.createTextNode(text);
				child.appendChild(node);
			}
		}

		return child;
	}

	private Template createTemplate(final Page page, final DOMNode parent, final String content) throws FrameworkException {

		final Template template = StructrApp.getInstance().create(Template.class,
			new NodeAttribute<>(Template.content, content),
			new NodeAttribute<>(Template.ownerDocument, page)
		);

		if (parent != null) {
			parent.appendChild((DOMNode)template);
		}

		return template;
	}

	private <T> T createContent(final Page page, final DOMNode parent, final String content) {

		final T child = (T)page.createTextNode(content);
		parent.appendChild((DOMNode)child);

		return child;
	}

	private <T> T createComment(final Page page, final DOMNode parent, final String comment) {

		final T child = (T)page.createComment(comment);
		parent.appendChild((DOMNode)child);

		return child;
	}

	private <T> T createComponent(final DOMNode node) throws FrameworkException {
		return (T) new CreateComponentCommand().create(node);
	}

	private <T> T cloneComponent(final DOMNode node, final DOMNode parentNode) throws FrameworkException {
		return (T) new CloneComponentCommand().cloneComponent(node, parentNode);
	}

	// ----- nested classes -----
	private static class DeletingFileVisitor implements FileVisitor<Path> {

		@Override
		public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
			return FileVisitResult.CONTINUE;
		}

		@Override
		public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {

			Files.delete(file);

			return FileVisitResult.CONTINUE;
		}

		@Override
		public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
			return FileVisitResult.CONTINUE;
		}

		@Override
		public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {

			Files.delete(dir);

			return FileVisitResult.CONTINUE;
		}

	}
}
