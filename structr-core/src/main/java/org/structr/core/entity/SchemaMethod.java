/**
 * Copyright (C) 2010-2017 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.core.entity;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.structr.common.PropertyView;
import org.structr.common.View;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.relationship.SchemaMethodParameters;
import org.structr.core.entity.relationship.SchemaNodeMethod;
import org.structr.core.notion.PropertySetNotion;
import org.structr.core.property.ArrayProperty;
import org.structr.core.property.BooleanProperty;
import org.structr.core.property.EndNodes;
import org.structr.core.property.Property;
import org.structr.core.property.StartNode;
import org.structr.core.property.StringProperty;
import org.structr.schema.SchemaHelper;
import org.structr.schema.action.ActionEntry;

/**
 *
 *
 */
public class SchemaMethod extends SchemaReloadingNode implements Favoritable {

	public static final Property<List<SchemaMethodParameter>> parameters = new EndNodes<>("parameters", SchemaMethodParameters.class);
	public static final Property<AbstractSchemaNode> schemaNode          = new StartNode<>("schemaNode", SchemaNodeMethod.class, new PropertySetNotion(AbstractNode.id, AbstractNode.name));
	public static final Property<String>             signature           = new StringProperty("signature").indexed();
	public static final Property<String>             virtualFileName     = new StringProperty("virtualFileName");
	public static final Property<String>             returnType          = new StringProperty("returnType");
	public static final Property<String>             source              = new StringProperty("source");
	public static final Property<String>             comment             = new StringProperty("comment");
	public static final Property<String[]>           exceptions          = new ArrayProperty("exceptions", String.class);
	public static final Property<Boolean>            callSuper           = new BooleanProperty("callSuper");
	public static final Property<Boolean>            overridesExisting   = new BooleanProperty("overridesExisting");
	public static final Property<String>             codeType            = new StringProperty("codeType");

	public static final View defaultView = new View(SchemaMethod.class, PropertyView.Public,
		name, schemaNode, source, comment, isFavoritable
	);

	public static final View uiView = new View(SchemaMethod.class, PropertyView.Ui,
		name, schemaNode, source, comment, isFavoritable
	);

	public static final View exportView = new View(SchemaMethod.class, "export",
		id, type, schemaNode, name, source, comment
	);

	public ActionEntry getActionEntry(final AbstractSchemaNode schemaEntity) throws FrameworkException {

		final ActionEntry entry  = new ActionEntry("___" + SchemaHelper.cleanPropertyName(getProperty(AbstractNode.name)), getProperty(SchemaMethod.source), getProperty(SchemaMethod.codeType));

		for (final SchemaMethodParameter parameter : getProperty(parameters)) {

			entry.addParameter(parameter.getParameterType(), parameter.getName());
		}

		entry.setReturnType(getProperty(returnType));
		entry.setCallSuper(getProperty(callSuper));

		final String[] _exceptions = getProperty(exceptions);
		if (_exceptions != null) {

			for (final String exception : _exceptions) {
				entry.addException(exception);
			}
		}

		// check for overridden methods and determine method signature etc. from superclass(es)
		if (getProperty(overridesExisting)) {
			determineSignature(schemaEntity, entry, getProperty(name));
		}

		return entry;
	}

	public boolean isJava() {
		return "java".equals(getProperty(codeType));
	}

	// ----- private methods -----
	private void addType(final Queue<String> typeQueue, final AbstractSchemaNode schemaNode) {

		final String _extendsClass = schemaNode.getProperty(SchemaNode.extendsClass);
		if (_extendsClass != null) {

			typeQueue.add(StringUtils.substringBefore(_extendsClass, "<"));
		}

		final String _interfaces = schemaNode.getProperty(SchemaNode.implementsInterfaces);
		if (_interfaces != null) {

			for (final String iface : _interfaces.split("[, ]+")) {

				typeQueue.add(iface);
			}
		}
	}

	private void determineSignature(final AbstractSchemaNode schemaEntity, final ActionEntry entry, final String methodName) throws FrameworkException {

		final App app                  = StructrApp.getInstance();
		final Set<String> visitedTypes = new LinkedHashSet<>();
		final Queue<String> typeQueue  = new LinkedList<>();
		final String structrPackage    = "org.structr.dynamic.";

		// initial type
		addType(typeQueue, schemaEntity);

		while (!typeQueue.isEmpty()) {

			final String typeName = typeQueue.poll();
			String shortTypeName  = typeName;

			if (typeName != null && !visitedTypes.contains(typeName)) {

				visitedTypes.add(typeName);

				if (typeName.startsWith(structrPackage)) {
					shortTypeName = typeName.substring(structrPackage.length());
				}

				// try to find schema node for the given type
				final SchemaNode typeNode = app.nodeQuery(SchemaNode.class).andName(shortTypeName).getFirst();
				if (typeNode != null && !typeNode.equals(schemaEntity)) {

					// try to identify overridden schema method from database
					final SchemaMethod superMethod = app.nodeQuery(SchemaMethod.class)
						.and(SchemaMethod.schemaNode, typeNode)
						.and(SchemaMethod.name, methodName)
						.getFirst();

					if (superMethod != null) {

						final ActionEntry superEntry = superMethod.getActionEntry(typeNode);

						entry.copy(superEntry);

						// done
						return;
					}

					// next type in queue
					addType(typeQueue, typeNode);

				} else {

					// no schema node for the given type found, try internal types
					final Class internalType = SchemaHelper.classForName(typeName);
					if (internalType != null) {

						if (getSignature(internalType, methodName, entry)) {

							return;
						}

						final Class superclass = internalType.getSuperclass();
						if (superclass != null) {

							// examine superclass as well
							typeQueue.add(superclass.getName());

							// collect interfaces
							for (final Class iface : internalType.getInterfaces()) {
								typeQueue.add(iface.getName());
							}
						}
					}
				}
			}
		}
	}

	// ----- interface Favoritable -----
	@Override
	public String getContext() {

		final AbstractSchemaNode parent = getProperty(SchemaMethod.schemaNode);
		final StringBuilder buf = new StringBuilder();

		if (parent != null) {

			buf.append(parent.getProperty(SchemaNode.name));
			buf.append(".");
			buf.append(getProperty(name));
		}

		return buf.toString();
	}

	@Override
	public String getFavoriteContent() {
		return getProperty(SchemaMethod.source);
	}

	@Override
	public String getFavoriteContentType() {
		return "application/x-structr-javascript";
	}

	@Override
	public void setFavoriteContent(String content) throws FrameworkException {
		setProperty(SchemaMethod.source, content);
	}

	private boolean getSignature(final Class type, final String methodName, final ActionEntry entry) {

		// superclass is AbstractNode
		for (final Method method : type.getMethods()) {

			if (methodName.equals(method.getName()) && (method.getModifiers() & Modifier.STATIC) == 0) {

				final Type returnType = method.getGenericReturnType();

				// check for generic return type, and if the method defines its own generic type
				if (returnType instanceof TypeVariable && ((TypeVariable)returnType).getGenericDeclaration().equals(method)) {

					// method defines its own generic type
					entry.setReturnType("<" + returnType.getTypeName() + "> " + returnType.getTypeName());

				} else {

					// non-generic return type
					final Class returnClass = method.getReturnType();
					if (returnClass.isArray()) {

						entry.setReturnType(returnClass.getComponentType().getName() + "[]");

					} else {

						entry.setReturnType(method.getReturnType().getName());
					}
				}

				for (final Parameter parameter : method.getParameters()) {

					String typeName = parameter.getParameterizedType().getTypeName();
					String name     = parameter.getType().getSimpleName();

					if (typeName.contains("$")) {
						typeName = typeName.replace("$", ".");
					}

					entry.addParameter(typeName, parameter.getName());
				}

				for (final Class exception : method.getExceptionTypes()) {
					entry.addException(exception.getName());
				}

				entry.setOverrides(getProperty(overridesExisting));
				entry.setCallSuper(getProperty(callSuper));

				// success
				return true;
			}
		}

		return false;
	}
}
