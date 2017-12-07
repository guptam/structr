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
package org.structr.schema.export;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.TreeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.entity.SchemaMethod;
import org.structr.core.entity.SchemaMethodParameter;
import org.structr.core.property.PropertyMap;
import org.structr.schema.json.JsonMethod;
import org.structr.schema.json.JsonParameter;
import org.structr.schema.json.JsonSchema;

/**
 *
 *
 */
public class StructrParameterDefinition implements JsonParameter, StructrDefinition {

	private static final Logger logger = LoggerFactory.getLogger(StructrParameterDefinition.class.getName());

	private JsonMethod parent = null;
	private String name       = null;
	private String type       = null;

	StructrParameterDefinition(final JsonMethod parent, final String name) {

		this.parent = parent;
		this.name   = name;
	}

	@Override
	public String toString() {
		return type + " " + name;
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}

	@Override
	public boolean equals(final Object other) {

		if (other instanceof StructrParameterDefinition) {

			return other.hashCode() == hashCode();
		}

		return false;
	}

	@Override
	public URI getId() {

		final URI parentId = parent.getId();
		if (parentId != null) {

			try {
				final URI containerURI = new URI(parentId.toString() + "/");
				return containerURI.resolve("properties/" + getName());

			} catch (URISyntaxException urex) {
				logger.warn("", urex);
			}
		}

		return null;
	}

	@Override
	public JsonMethod getParent() {
		return parent;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public JsonParameter setName(String name) {

		this.name = name;
		return this;
	}

	@Override
	public String getType() {
		return type;
	}

	@Override
	public JsonParameter setType(final String type) {
		this.type = type;
		return this;
	}

	@Override
	public int compareTo(final JsonParameter o) {
		return getName().compareTo(o.getName());
	}

	@Override
	public StructrDefinition resolveJsonPointerKey(final String key) {
		return null;
	}

	// ----- package methods -----
	SchemaMethodParameter createDatabaseSchema(final App app, final SchemaMethod schemaMethod) throws FrameworkException {

		final PropertyMap getOrCreateProperties = new PropertyMap();
		final PropertyMap updateProperties      = new PropertyMap();

		getOrCreateProperties.put(SchemaMethodParameter.name,         getName());
		getOrCreateProperties.put(SchemaMethodParameter.schemaMethod, schemaMethod);

		SchemaMethodParameter parameter = app.nodeQuery(SchemaMethodParameter.class).and(getOrCreateProperties).getFirst();
		if (parameter == null) {

			parameter = app.create(SchemaMethodParameter.class, getOrCreateProperties);
		}

		updateProperties.put(SchemaMethodParameter.parameterType, type);

		// update properties
		parameter.setProperties(SecurityContext.getSuperUserInstance(), updateProperties);

		// return modified property
		return parameter;
	}


	void deserialize(final Map<String, Object> source) {

		final Object _type = source.get(JsonSchema.KEY_PARAMETER_TYPE);
		if (_type != null && _type instanceof String) {

			this.type = (String)_type;
		}
	}

	void deserialize(final SchemaMethodParameter method) {

		setName(method.getName());
		setType(method.getProperty(SchemaMethodParameter.parameterType));
	}

	Map<String, Object> serialize() {

		final Map<String, Object> map = new TreeMap<>();

		map.put(JsonSchema.KEY_PARAMETER_TYPE, type);

		return map;
	}

	void initializeReferences() {
	}

	// ----- static methods -----
	static StructrParameterDefinition deserialize(final StructrMethodDefinition parent, final String name, final Map<String, Object> source) {

		final StructrParameterDefinition newParameter = new StructrParameterDefinition(parent, name);

		newParameter.deserialize(source);

		return newParameter;
	}

	static StructrParameterDefinition deserialize(final StructrMethodDefinition parent, final SchemaMethodParameter parameter) {

		final StructrParameterDefinition newParameter = new StructrParameterDefinition(parent, parameter.getName());

		newParameter.deserialize(parameter);

		return newParameter;
	}
}
