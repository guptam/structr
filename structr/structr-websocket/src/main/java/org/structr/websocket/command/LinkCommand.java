/*
 *  Copyright (C) 2011 Axel Morgner
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

import org.structr.common.RelType;
import org.structr.common.SecurityContext;
import org.structr.core.Command;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.DirectedRelationship;
import org.structr.core.entity.DirectedRelationship.Cardinality;
import org.structr.core.node.CreateNodeCommand;
import org.structr.core.node.NodeAttribute;
import org.structr.core.node.StructrTransaction;
import org.structr.core.node.TransactionCommand;
import org.structr.websocket.message.MessageBuilder;
import org.structr.websocket.message.WebSocketMessage;

//~--- JDK imports ------------------------------------------------------------

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author Axel Morgner
 */
public class LinkCommand extends AbstractCommand {

	@Override
	public void processMessage(WebSocketMessage webSocketData) {

		SecurityContext securityContext = SecurityContext.getSuperUserInstance();

		// create static relationship
		String sourceId                = webSocketData.getId();
		Map<String, Object> properties = webSocketData.getData();
		String resourceId              = (String) properties.get("resourceId");
		String rootResourceId          = (String) properties.get("rootResourceId");

		if (rootResourceId == null) {

			rootResourceId = "*";

		}

		Integer startOffset = Integer.parseInt((String) properties.get("startOffset"));
		Integer endOffset   = Integer.parseInt((String) properties.get("endOffset"));

		properties.remove("id");

		if ((sourceId != null) && (resourceId != null)) {

			AbstractNode sourceNode   = getNode(sourceId);
			AbstractNode resourceNode = getNode(resourceId);

			if ((sourceNode != null) && (resourceNode != null)) {

				try {

					Command transactionCommand = Services.command(SecurityContext.getSuperUserInstance(), TransactionCommand.class);

					// Create first (leading) node
					final List<NodeAttribute> attrsFirstNode = new LinkedList<NodeAttribute>();

					attrsFirstNode.add(new NodeAttribute(AbstractNode.Key.type.name(), "Content"));
					attrsFirstNode.add(new NodeAttribute(AbstractNode.Key.name.name(), "First Node"));

					if (sourceNode.getType().equals("Content")) {

						String content = sourceNode.getStringProperty("content");

						attrsFirstNode.add(new NodeAttribute("content", content.substring(0, startOffset)));

					}


					StructrTransaction transaction = new StructrTransaction() {

						@Override
						public Object execute() throws Throwable {
							return Services.command(SecurityContext.getSuperUserInstance(), CreateNodeCommand.class).execute(attrsFirstNode);
						}
					};
					AbstractNode firstNode = (AbstractNode) transactionCommand.execute(transaction);

					// Create second (linked) node
					final List<NodeAttribute> attrsSecondNode = new LinkedList<NodeAttribute>();

					attrsSecondNode.add(new NodeAttribute(AbstractNode.Key.type.name(), "Content"));
					attrsSecondNode.add(new NodeAttribute(AbstractNode.Key.name.name(), "Second (Link) Node"));

					if (sourceNode.getType().equals("Content")) {

						String content = sourceNode.getStringProperty("content");

						attrsSecondNode.add(new NodeAttribute("content", content.substring(startOffset, endOffset)));

					}

					transaction = new StructrTransaction() {

						@Override
						public Object execute() throws Throwable {
							return Services.command(SecurityContext.getSuperUserInstance(), CreateNodeCommand.class).execute(attrsSecondNode);
						}
					};

					AbstractNode secondNode = (AbstractNode) transactionCommand.execute(transaction);

					// Create third (trailing) node
					final List<NodeAttribute> attrsThirdNode = new LinkedList<NodeAttribute>();

					attrsThirdNode.add(new NodeAttribute(AbstractNode.Key.type.name(), "Content"));
					attrsThirdNode.add(new NodeAttribute(AbstractNode.Key.name.name(), "Third Node"));

					if (sourceNode.getType().equals("Content")) {

						String content = sourceNode.getStringProperty("content");

						attrsThirdNode.add(new NodeAttribute("content", content.substring(endOffset, content.length())));

					}

					transaction = new StructrTransaction() {

						@Override
						public Object execute() throws Throwable {
							return Services.command(SecurityContext.getSuperUserInstance(), CreateNodeCommand.class).execute(attrsThirdNode);
						}
					};

					AbstractNode thirdNode = (AbstractNode) transactionCommand.execute(transaction);

					// Create a CONTAINS relationship
					DirectedRelationship rel     = new DirectedRelationship(null, RelType.CONTAINS, Direction.OUTGOING, Cardinality.ManyToMany, null);
					Map<String, Object> relProps = new HashMap<String, Object>();

					relProps.put(rootResourceId, 0);
					rel.createRelationship(securityContext, sourceNode, firstNode, relProps);
					relProps.put(rootResourceId, 1);
					rel.createRelationship(securityContext, sourceNode, secondNode, relProps);
					relProps.put(rootResourceId, 2);
					rel.createRelationship(securityContext, sourceNode, thirdNode, relProps);

					// Create a LINK relationship
					rel = new DirectedRelationship(resourceNode.getType(), RelType.LINK, Direction.OUTGOING, Cardinality.ManyToMany, null);

					rel.createRelationship(securityContext, secondNode, resourceNode);
					sourceNode.setType("Element");
					sourceNode.removeProperty("content");

				} catch (Throwable t) {
					getWebSocket().send(MessageBuilder.status().code(400).message(t.getMessage()).build(), true);
				}

			} else {

				getWebSocket().send(MessageBuilder.status().code(404).build(), true);

			}

		} else {

			getWebSocket().send(MessageBuilder.status().code(400).message("The LINK command needs id and data.id!").build(), true);

		}
	}

	//~--- get methods ----------------------------------------------------

	@Override
	public String getCommand() {
		return "LINK";
	}
}
