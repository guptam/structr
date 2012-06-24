/*
 *  Copyright (C) 2010-2012 Axel Morgner
 *
 *  This file is part of structr <http://structr.org>.
 *
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */



package org.structr.websocket.command;

import org.neo4j.graphdb.Direction;

import org.structr.common.GraphObjectComparator;
import org.structr.common.RelType;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.Command;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.node.DeleteRelationshipCommand;
import org.structr.core.node.StructrTransaction;
import org.structr.core.node.TransactionCommand;
import org.structr.core.node.search.Search;
import org.structr.core.node.search.SearchAttribute;
import org.structr.core.node.search.SearchNodeCommand;
import org.structr.web.common.RelationshipHelper;
import org.structr.web.entity.Page;
import org.structr.websocket.message.MessageBuilder;
import org.structr.websocket.message.WebSocketMessage;

//~--- JDK imports ------------------------------------------------------------

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author Christian Morgner
 * @author Axel Morgner
 */
public class RemoveCommand extends AbstractCommand {

	@Override
	public void processMessage(WebSocketMessage webSocketData) {

		final SecurityContext securityContext = getWebSocket().getSecurityContext();

		// create static relationship
		String id                = webSocketData.getId();
		String parentId          = (String) webSocketData.getNodeData().get("id");
		final String componentId = (String) webSocketData.getNodeData().get("componentId");
		final String pageId      = (String) webSocketData.getNodeData().get("pageId");
		String position          = (String) webSocketData.getNodeData().get("position");

		if ((id != null) && (parentId != null)) {

			final AbstractNode nodeToRemove = getNode(id);
			final AbstractNode parentNode   = getNode(parentId);
			final Long pos                  = (position != null)
							  ? Long.parseLong(position)
							  : null;

			if ((nodeToRemove != null) && (parentNode != null)) {

				final List<AbstractRelationship> rels = parentNode.getRelationships(RelType.CONTAINS, Direction.OUTGOING);
				StructrTransaction transaction        = new StructrTransaction() {

					@Override
					public Object execute() throws FrameworkException {

						String removedRelId                      = null;
						Command deleteRel                        = Services.command(securityContext, DeleteRelationshipCommand.class);
						boolean hasPageId                        = true;
						List<AbstractRelationship> relsToReorder = new ArrayList<AbstractRelationship>();

						for (AbstractRelationship rel : rels) {

							if (pageId == null || rel.getProperty(pageId) != null) {

								relsToReorder.add(rel);
							}

							if (rel.getEndNode().equals(nodeToRemove) && ((componentId == null) || componentId.equals(rel.getStringProperty("componentId")))
								&& ((pageId == null) || (rel.getProperty(pageId) != null))) {

								if (pos == null) {

									deleteRel.execute(rel);
								} else {

									if (pos.equals(rel.getLongProperty(pageId))) {

										rel.removeProperty(pageId);
										RelationshipHelper.untagOutgoingRelsFromPageId(nodeToRemove, nodeToRemove, pageId, pageId);

										hasPageId = hasPageIds(securityContext, rel);

										// If no pageId property is left, remove relationship
										if (!hasPageId) {

											relsToReorder.remove(rel);
											deleteRel.execute(rel);

											break;

										}

									}

								}

							}

						}

						// Always re-order relationships
						reorderRels(relsToReorder, pageId);

						return null;

					}

				};

				// execute transaction
				try {

					Services.command(securityContext, TransactionCommand.class).execute(transaction);

				} catch (FrameworkException fex) {

					getWebSocket().send(MessageBuilder.status().code(400).message(fex.getMessage()).build(), true);

				}

			} else {

				getWebSocket().send(MessageBuilder.status().code(404).build(), true);
			}

		} else {

			getWebSocket().send(MessageBuilder.status().code(400).message("Add needs id and data.id!").build(), true);
		}

	}

	private void reorderRels(final List<AbstractRelationship> rels, final String pageId) throws FrameworkException {

		long i = 0;

		Collections.sort(rels, new GraphObjectComparator(pageId, GraphObjectComparator.ASCENDING));

		for (AbstractRelationship rel : rels) {

			try {

				rel.getId();
				rel.setProperty(pageId, i);

				i++;

			} catch (IllegalStateException ise) {

				// Silently ignore this exception and continue, omitting deleted rels
				continue;
			}

		}

	}

	//~--- get methods ----------------------------------------------------

	@Override
	public String getCommand() {

		return "REMOVE";

	}

	private boolean hasPageIds(final SecurityContext securityContext, final AbstractRelationship rel) throws FrameworkException {

		Command searchNode = Services.command(securityContext, SearchNodeCommand.class);
		long count         = 0;

		for (Entry entry : rel.getProperties().entrySet()) {

			String key = (String) entry.getKey();

			// Object val = entry.getValue();
			// Check if key is a node id (UUID format)
			if (key.matches("[a-zA-Z0-9]{32}")) {

				List<SearchAttribute> attrs = new LinkedList<SearchAttribute>();

				attrs.add(Search.andExactType(Page.class.getSimpleName()));
				attrs.add(Search.andExactUuid(key));

				List<AbstractNode> results = (List<AbstractNode>) searchNode.execute(null, false, false, attrs);

				if (results != null && !results.isEmpty()) {

					count++;
				} else {

					// UUID, but not page found: Remove this property
					rel.removeProperty(key);
				}

			}

		}

		return count > 0;

	}

}
