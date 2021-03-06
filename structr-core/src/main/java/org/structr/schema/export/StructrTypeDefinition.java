/**
 * Copyright (C) 2010-2019 Structr GmbH
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
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.common.util.UrlUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.SecurityContext;
import org.structr.common.View;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractSchemaNode;
import org.structr.core.entity.SchemaMethod;
import org.structr.core.entity.SchemaNode;
import org.structr.core.entity.SchemaProperty;
import org.structr.core.entity.SchemaRelationshipNode;
import org.structr.core.entity.SchemaView;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.property.PropertyMap;
import org.structr.schema.SchemaService;
import org.structr.schema.json.JsonBooleanArrayProperty;
import org.structr.schema.json.JsonBooleanProperty;
import org.structr.schema.json.JsonDateArrayProperty;
import org.structr.schema.json.JsonDateProperty;
import org.structr.schema.json.JsonDynamicProperty;
import org.structr.schema.json.JsonEnumProperty;
import org.structr.schema.json.JsonFunctionProperty;
import org.structr.schema.json.JsonIntegerArrayProperty;
import org.structr.schema.json.JsonIntegerProperty;
import org.structr.schema.json.JsonLongArrayProperty;
import org.structr.schema.json.JsonLongProperty;
import org.structr.schema.json.JsonMethod;
import org.structr.schema.json.JsonNumberArrayProperty;
import org.structr.schema.json.JsonNumberProperty;
import org.structr.schema.json.JsonProperty;
import org.structr.schema.json.JsonReferenceProperty;
import org.structr.schema.json.JsonSchema;
import org.structr.schema.json.JsonScriptProperty;
import org.structr.schema.json.JsonStringArrayProperty;
import org.structr.schema.json.JsonStringProperty;
import org.structr.schema.json.JsonType;

/**
 * @param <T>
 */
public abstract class StructrTypeDefinition<T extends AbstractSchemaNode> implements JsonType, StructrDefinition {

	private static final Logger logger = LoggerFactory.getLogger(StructrTypeDefinition.class);

	protected final Set<StructrPropertyDefinition> properties     = new TreeSet<>();
	protected final Map<String, Set<String>> views                = new TreeMap<>();
	protected final Map<String, String> viewOrder                 = new TreeMap<>();
	protected final List<StructrMethodDefinition> methods         = new LinkedList<>();
	protected final Set<URI> implementedInterfaces                = new TreeSet<>();
	protected boolean isInterface                                 = false;
	protected boolean isAbstract                                  = false;
	protected boolean isBuiltinType                               = false;
	protected StructrSchemaDefinition root                        = null;
	protected URI baseTypeReference                               = null;
	protected String category                                     = null;
	protected String name                                         = null;
	protected T schemaNode                                        = null;

	StructrTypeDefinition(final StructrSchemaDefinition root, final String name) {

		this.root = root;
		this.name = name;
	}

	abstract T createSchemaNode(final Map<String, SchemaNode> schemaNodes, final App app, final PropertyMap createProperties) throws FrameworkException;

	@Override
	public String toString() {
		return "StructrTypeDefinition(" + name + ")";
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}

	@Override
	public boolean equals(final Object other) {

		if (other instanceof StructrPropertyDefinition) {

			return other.hashCode() == hashCode();
		}

		return false;
	}

	@Override
	public URI getId() {

		final URI id  = root.getId();
		final URI uri = URI.create("definitions/" + getName());

		return id.resolve(uri);
	}

	@Override
	public JsonSchema getSchema() {
		return root;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public JsonType setCategory(final String category) {

		this.category = category;
		return this;
	}

	@Override
	public String getCategory() {
		return category;
	}

	@Override
	public JsonType setName(final String name) {

		this.name = name;
		return this;
	}

	@Override
	public boolean isInterface() {
		return isInterface;
	}

	@Override
	public JsonType setIsInterface() {
		this.isInterface = true;
		return this;
	}

	@Override
	public boolean isAbstract() {
		return isAbstract;
	}

	@Override
	public JsonType setIsAbstract() {
		this.isAbstract = true;
		return this;
	}

	@Override
	public JsonMethod addMethod(final String name, final String source, final String comment) {

		final StructrMethodDefinition newMethod = new StructrMethodDefinition(this, name);

		newMethod.setSource(source);
		newMethod.setComment(comment);

		methods.add(newMethod);

		// sort methods
		Collections.sort(methods);

		return newMethod;
	}

	@Override
	public JsonMethod addMethod(final String name) {

		final StructrMethodDefinition newMethod = new StructrMethodDefinition(this, name);

		newMethod.setCodeType("java");

		methods.add(newMethod);

		// sort methods
		Collections.sort(methods);

		return newMethod;
	}

	@Override
	public JsonMethod overrideMethod(final String name, final boolean callSuper, final String implementation) {

		final StructrMethodDefinition newMethod = new StructrMethodDefinition(this, name);

		newMethod.setSource(implementation);
		newMethod.setOverridesExisting(true);
		newMethod.setCallSuper(callSuper);
		newMethod.setCodeType("java");

		methods.add(newMethod);

		// sort methods
		Collections.sort(methods);

		return newMethod;
	}

	@Override
	public JsonMethod addPropertyGetter(final String propertyName, final Class type) {

		final StructrMethodDefinition newMethod = new StructrMethodDefinition(this, "get" + StringUtils.capitalize(propertyName));

		newMethod.setSource("return getProperty(" + propertyName + "Property);");
		newMethod.setReturnType(type.getName().replace("$", "."));
		newMethod.setCodeType("java");

		methods.add(newMethod);

		// sort methods
		Collections.sort(methods);

		return newMethod;
	}

	@Override
	public JsonMethod addPropertySetter(final String propertyName, final Class type) {

		final StructrMethodDefinition newMethod = new StructrMethodDefinition(this, "set" + StringUtils.capitalize(propertyName));

		newMethod.setSource("setProperty(" + propertyName + "Property, value);");
		newMethod.addParameter("value", type.getName());
		newMethod.setCodeType("java");
		newMethod.addException("FrameworkException");

		methods.add(newMethod);

		// sort methods
		Collections.sort(methods);

		return newMethod;
	}

	@Override
	public JsonType setExtends(final JsonType superType) {

		this.baseTypeReference = superType.getId();
		return this;

	}

	@Override
	public JsonType setExtends(final URI externalReference) {

		this.baseTypeReference = externalReference;
		return this;
	}

	@Override
	public URI getExtends() {
		return baseTypeReference;
	}

	@Override
	public JsonType setImplements(final URI uri) {

		implementedInterfaces.add(uri);
		return this;
	}

	@Override
	public Set<URI> getImplements() {
		return implementedInterfaces;
	}

	@Override
	public Set<JsonProperty> getProperties() {
		return (Set)properties;
	}

	@Override
	public List<JsonMethod> getMethods() {
		return (List)methods;
	}

	@Override
	public Set<String> getViewNames() {
		return views.keySet();
	}

	@Override
	public Set<String> getViewPropertyNames(final String viewName) {
		return views.get(viewName);
	}

	@Override
	public JsonType addViewProperty(final String viewName, final String propertyName) {

		addPropertyNameToViews(propertyName, viewName);
		return this;
	}

	@Override
	public Set<String> getRequiredProperties() {

		final Set<String> requiredProperties = new TreeSet<>();

		for (final StructrPropertyDefinition property : properties) {

			if (property.isRequired()) {

				requiredProperties.add(property.getName());
			}
		}

		return requiredProperties;
	}

	@Override
	public JsonStringProperty addStringProperty(final String name, final String... views) {

		final StructrStringProperty stringProperty = new StructrStringProperty(this, name);

		addPropertyNameToViews(name, views);

		properties.add(stringProperty);

		return stringProperty;
	}

	@Override
	public JsonStringProperty addPasswordProperty(final String name, final String... views) {

		final StructrPasswordProperty passwordProperty = new StructrPasswordProperty(this, name);

		addPropertyNameToViews(name, views);

		properties.add(passwordProperty);

		return passwordProperty;
	}

	@Override
	public JsonStringArrayProperty addStringArrayProperty(final String name, final String... views) {

		final StructrStringArrayProperty stringArrayProperty = new StructrStringArrayProperty(this, name);

		addPropertyNameToViews(name, views);

		properties.add(stringArrayProperty);

		return stringArrayProperty;
	}

	@Override
	public JsonDateArrayProperty addDateArrayProperty(final String name, final String... views) {

		final StructrDateArrayProperty dateArrayProperty = new StructrDateArrayProperty(this, name);

		addPropertyNameToViews(name, views);

		properties.add(dateArrayProperty);

		return dateArrayProperty;
	}

	@Override
	public JsonDateProperty addDateProperty(final String name, final String... views) {

		final StructrDateProperty dateProperty = new StructrDateProperty(this, name);

		addPropertyNameToViews(name, views);

		properties.add(dateProperty);

		return dateProperty;

	}

	@Override
	public JsonIntegerProperty addIntegerProperty(final String name, final String... views) {

		final StructrIntegerProperty numberProperty = new StructrIntegerProperty(this, name);

		addPropertyNameToViews(name, views);

		properties.add(numberProperty);

		return numberProperty;
	}

	@Override
	public JsonIntegerArrayProperty addIntegerArrayProperty(final String name, final String... views) {

		final StructrIntegerArrayProperty numberProperty = new StructrIntegerArrayProperty(this, name);

		addPropertyNameToViews(name, views);

		properties.add(numberProperty);

		return numberProperty;
	}

	@Override
	public JsonLongProperty addLongProperty(final String name, final String... views) {

		final StructrLongProperty numberProperty = new StructrLongProperty(this, name);

		addPropertyNameToViews(name, views);

		properties.add(numberProperty);

		return numberProperty;
	}

	@Override
	public JsonLongArrayProperty addLongArrayProperty(final String name, final String... views) {

		final StructrLongArrayProperty numberProperty = new StructrLongArrayProperty(this, name);

		addPropertyNameToViews(name, views);

		properties.add(numberProperty);

		return numberProperty;
	}

	@Override
	public JsonNumberProperty addNumberProperty(final String name, final String... views) {

		final StructrNumberProperty numberProperty = new StructrNumberProperty(this, name);

		addPropertyNameToViews(name, views);

		properties.add(numberProperty);

		return numberProperty;
	}

	@Override
	public JsonNumberArrayProperty addDoubleArrayProperty(final String name, final String... views) {

		final StructrNumberArrayProperty numberArrayProperty = new StructrNumberArrayProperty(this, name);

		addPropertyNameToViews(name, views);

		properties.add(numberArrayProperty);

		return numberArrayProperty;
	}

	@Override
	public JsonBooleanProperty addBooleanProperty(final String name, final String... views) {

		final StructrBooleanProperty booleanProperty = new StructrBooleanProperty(this, name);

		addPropertyNameToViews(name, views);

		properties.add(booleanProperty);

		return booleanProperty;
	}

	@Override
	public JsonBooleanArrayProperty addBooleanArrayProperty(final String name, final String... views) {

		final StructrBooleanArrayProperty booleanArrayProperty = new StructrBooleanArrayProperty(this, name);

		addPropertyNameToViews(name, views);

		properties.add(booleanArrayProperty);

		return booleanArrayProperty;
	}

	@Override
	public JsonScriptProperty addScriptProperty(final String name, final String... views) {

		final StructrScriptProperty scriptProperty = new StructrScriptProperty(this, name);

		addPropertyNameToViews(name, views);

		properties.add(scriptProperty);

		return scriptProperty;
	}

	@Override
	public JsonFunctionProperty addFunctionProperty(final String name, final String... views) {

		final StructrFunctionProperty functionProperty = new StructrFunctionProperty(this, name);

		addPropertyNameToViews(name, views);

		properties.add(functionProperty);

		return functionProperty;
	}

	@Override
	public JsonEnumProperty addEnumProperty(final String name, final String... views) {

		final StructrEnumProperty enumProperty = new StructrEnumProperty(this, name);

		addPropertyNameToViews(name, views);

		properties.add(enumProperty);

		return enumProperty;
	}

	@Override
	public JsonDynamicProperty addCustomProperty(final String name, final String fqcn, final String... views) {

		final StructrCustomProperty customProperty = new StructrCustomProperty(this, name);

		customProperty.setFqcn(fqcn);

		addPropertyNameToViews(name, views);

		properties.add(customProperty);

		return customProperty;
	}

	@Override
	public JsonReferenceProperty addReferenceProperty(final String name, final JsonReferenceProperty referencedProperty, final String... views) {

		final String reference = root.toJsonPointer(referencedProperty.getId());
		final String refType   = referencedProperty.getType();
		final String refName   = referencedProperty.getName();

		final StructrReferenceProperty ref = new NotionReferenceProperty(this, name, reference, refType, refName);

		addPropertyNameToViews(name, views);

		properties.add(ref);

		return ref;
	}

	@Override
	public JsonReferenceProperty addIdReferenceProperty(final String name, final JsonReferenceProperty referencedProperty, final String... views) {

		final String reference = root.toJsonPointer(referencedProperty.getId());
		final String refType   = referencedProperty.getType();
		final String refName   = referencedProperty.getName();

		final StructrReferenceProperty ref = new IdNotionReferenceProperty(this, name, reference, refType, refName);

		addPropertyNameToViews(name, views);

		properties.add(ref);

		return ref;
	}

	@Override
	public int compareTo(final JsonType o) {
		return getName().compareTo(o.getName());
	}

	@Override
	public StructrDefinition resolveJsonPointerKey(final String key) {

		switch (key) {

			case JsonSchema.KEY_PROPERTIES:
				return new StructrDefinition() {

					@Override
					public StructrDefinition resolveJsonPointerKey(final String key) {

						for (final StructrPropertyDefinition property : properties) {

							if (key.equals(property.getName())) {

								return property;
							}
						}

						return null;
					}
				};

			case JsonSchema.KEY_VIEWS:
				return new StructrDefinition() {

					@Override
					public StructrDefinition resolveJsonPointerKey(final String key) {
						return null;
					}
				};
		}

		return null;
	}

	// ----- package methods -----
	Map<String, Object> serialize() {

		final Map<String, Object> serializedForm       = new TreeMap<>();
		final Map<String, Object> serializedProperties = new TreeMap<>();
		final Map<String, Object> serializedMethods    = new TreeMap<>();

		// populate properties
		for (final StructrPropertyDefinition property : properties) {
			serializedProperties.put(property.getName(), property.serialize());
		}

		// populate methods
		for (final StructrMethodDefinition method : methods) {

			final String name = method.getName();

			if (serializedMethods.containsKey(name)) {

				// Name is already present in the map, so there are at least
				// two method with the same name.
				final Object currentValue = serializedMethods.get(name);
				if (currentValue instanceof Collection) {

					// add serialized method to collection
					final Collection collection = (Collection)currentValue;

					collection.add(method.serialize());

				} else if (currentValue instanceof Map) {

					// remove map, add collection, add
					// map to collection
					final Map<String, Object> otherMethod   = (Map<String, Object>)serializedMethods.get(name);
					final Map<String, Object> currentMethod = method.serialize();
					final List<Map<String, Object>> list    = new LinkedList<>();

					list.add(otherMethod);
					list.add(currentMethod);

					serializedMethods.put(name, list);

				} else {

					logger.warn("Invalid storage datastructure for methods: {}", currentValue.getClass().getName());
				}

			} else {

				serializedMethods.put(method.getName(), method.serialize());
			}
		}

		serializedForm.put(JsonSchema.KEY_TYPE, "object");
		serializedForm.put(JsonSchema.KEY_IS_ABSTRACT, isAbstract);
		serializedForm.put(JsonSchema.KEY_IS_INTERFACE, isInterface);
		if (getClass().equals(StructrNodeTypeDefinition.class)) {
			serializedForm.put(JsonSchema.KEY_IS_BUILTIN_TYPE, isBuiltinType);
		}

		// properties
		if (!serializedProperties.isEmpty()) {
			serializedForm.put(JsonSchema.KEY_PROPERTIES, serializedProperties);
		}

		// required
		final Set<String> requiredProperties = getRequiredProperties();
		if (!requiredProperties.isEmpty()) {
			serializedForm.put(JsonSchema.KEY_REQUIRED, requiredProperties);
		}

		// views
		if (!views.isEmpty()) {

			// FIXME: views are different

			serializedForm.put(JsonSchema.KEY_VIEWS, views);
			serializedForm.put(JsonSchema.KEY_VIEW_ORDER, viewOrder);
		}

		// methods
		if (!serializedMethods.isEmpty()) {
			serializedForm.put(JsonSchema.KEY_METHODS, serializedMethods);
		}

		final URI ext = getExtends();
		if (ext != null) {

			serializedForm.put(JsonSchema.KEY_EXTENDS, root.toJsonPointer(ext));
		}

		if (!implementedInterfaces.isEmpty()) {

			final Set<String> interfaces = new TreeSet<>();

			for (final URI uri : implementedInterfaces) {
				interfaces.add(root.toJsonPointer(uri));
			}

			if (!interfaces.isEmpty()) {
				serializedForm.put(JsonSchema.KEY_IMPLEMENTS, interfaces);
			}
		}

		if (StringUtils.isNotBlank(category)) {
			serializedForm.put(JsonSchema.KEY_CATEGORY, category);
		}

		return serializedForm;
	}

	void deserialize(final Map<String, Object> source) {

		if (source.containsKey(JsonSchema.KEY_IS_ABSTRACT)) {
			this.isAbstract = (Boolean)source.get(JsonSchema.KEY_IS_ABSTRACT);
		}

		if (source.containsKey(JsonSchema.KEY_IS_INTERFACE)) {
			this.isInterface = (Boolean)source.get(JsonSchema.KEY_IS_INTERFACE);
		}

		if (source.containsKey(JsonSchema.KEY_IS_BUILTIN_TYPE)) {
			this.isBuiltinType = (Boolean)source.get(JsonSchema.KEY_IS_BUILTIN_TYPE);
		}

		if (source.containsKey(JsonSchema.KEY_EXTENDS)) {

			final Object extendsValue = source.get(JsonSchema.KEY_EXTENDS);

			// "old" schema
			String jsonPointerFormat = (String)extendsValue;
			if (jsonPointerFormat.startsWith("#")) {

				jsonPointerFormat = jsonPointerFormat.substring(1);
			}

			this.baseTypeReference = root.getId().relativize(URI.create(jsonPointerFormat));
		}

		if (source.containsKey(JsonSchema.KEY_IMPLEMENTS)) {

			final Object implementsValue = source.get(JsonSchema.KEY_IMPLEMENTS);
			if (implementsValue instanceof List) {

				// "new" schema
				final List<String> impl = (List<String>)implementsValue;
				for (String jsonPointerFormat : impl) {

					if (jsonPointerFormat.startsWith("#")) {

						jsonPointerFormat = jsonPointerFormat.substring(1);
					}

					this.implementedInterfaces.add(root.getId().relativize(URI.create(jsonPointerFormat)));
				}
			}
		}
	}

	void deserialize(final Map<String, SchemaNode> schemaNodes, final T schemaNode) {

		for (final SchemaProperty property : schemaNode.getProperty(AbstractSchemaNode.schemaProperties)) {

			final StructrPropertyDefinition propertyDefinition = StructrPropertyDefinition.deserialize(schemaNodes, this, property);
			if (propertyDefinition != null) {

				properties.add(propertyDefinition);
			}
		}

		for (final SchemaView view : schemaNode.getProperty(AbstractSchemaNode.schemaViews)) {

			if (!View.INTERNAL_GRAPH_VIEW.equals(view.getName())) {

				final Set<String> propertySet = new TreeSet<>();
				for (final SchemaProperty property : view.getProperty(SchemaView.schemaProperties)) {
					propertySet.add(property.getName());
				}

				final String nonGraphProperties = view.getProperty(SchemaView.nonGraphProperties);
				if (nonGraphProperties != null) {

					for (final String property : nonGraphProperties.split("[, ]+")) {
						final String trimmed = property.trim();

						if (StringUtils.isNotBlank(trimmed)) {
							propertySet.add(trimmed);
						}
					}
				}

				if (!propertySet.isEmpty()) {
					views.put(view.getName(), propertySet);

					final String order = view.getProperty(SchemaView.sortOrder);
					if (order != null) {
						viewOrder.put(view.getName(), order);
					}
				}
			}
		}

		for (final SchemaMethod method : schemaNode.getProperty(AbstractSchemaNode.schemaMethods)) {

			final StructrMethodDefinition newMethod = StructrMethodDefinition.deserialize(this, method);
			if (newMethod != null) {

				methods.add(newMethod);
			}
		}

		// $extends
		final String extendsClass = schemaNode.getProperty(SchemaNode.extendsClass);
		if (extendsClass != null) {

			final String typeName = resolveParameterizedType(extendsClass);

			if (extendsClass.startsWith("org.structr.dynamic.")) {

				this.baseTypeReference = root.getId().resolve("definitions/" + typeName);

			} else {

				this.baseTypeReference = StructrApp.getSchemaBaseURI().resolve("definitions/" + typeName);
			}
		}

		// $implements
		final String implementsInterfaces = schemaNode.getProperty(SchemaNode.implementsInterfaces);
		if (implementsInterfaces != null) {

			for (final String impl : implementsInterfaces.split("[, ]+")) {

				final String trimmed = impl.trim();
				if (StringUtils.isNotEmpty(trimmed)) {

					final String typeName = trimmed.substring(trimmed.lastIndexOf(".") + 1);
					if (trimmed.startsWith("org.structr.dynamic.")) {

						this.implementedInterfaces.add(root.getId().resolve("definitions/" + typeName));

					} else {

						this.implementedInterfaces.add(StructrApp.getSchemaBaseURI().resolve("definitions/" + typeName));
					}
				}
			}
		}

		this.isInterface    = schemaNode.getProperty(SchemaNode.isInterface);
		this.isAbstract     = schemaNode.getProperty(SchemaNode.isAbstract);
		this.isBuiltinType  = schemaNode.getProperty(SchemaNode.isBuiltinType);
		this.category       = schemaNode.getProperty(SchemaNode.category);

		if (this.category == null && getClass().equals(StructrNodeTypeDefinition.class)) {

			final JsonType type = SchemaService.getDynamicSchema().getType(this.getName(), false);
			if (type != null) {

				this.category = type.getCategory();
			}
		}
	}

	AbstractSchemaNode createDatabaseSchema(final Map<String, SchemaNode> schemaNodes, final App app) throws FrameworkException {

		final Map<String, SchemaProperty> schemaProperties = new TreeMap<>();
		final Map<String, SchemaMethod> schemaMethods      = new TreeMap<>();
		final PropertyMap createProperties                 = new PropertyMap();
		final PropertyMap nodeProperties                   = new PropertyMap();

		// properties that always need to be set
		createProperties.put(SchemaNode.isInterface, isInterface);
		createProperties.put(SchemaNode.isAbstract, isAbstract);
		createProperties.put(SchemaNode.category, category);
		createProperties.put(SchemaNode.isBuiltinType, isBuiltinType || SchemaService.DynamicSchemaRootURI.equals(root.getId()));

		final T schemaNode = createSchemaNode(schemaNodes, app, createProperties);

		for (final StructrPropertyDefinition property : properties) {

			final SchemaProperty schemaProperty = property.createDatabaseSchema(app, schemaNode);
			if (schemaProperty != null) {

				schemaProperties.put(schemaProperty.getName(), schemaProperty);
			}
		}

		// create views and associate the properties
		for (final Entry<String, Set<String>> view : views.entrySet()) {

			final List<SchemaProperty> viewProperties = new LinkedList<>();
			final List<String> nonGraphProperties     = new LinkedList<>();

			for (final String propertyName : view.getValue()) {

				final SchemaProperty property = schemaProperties.get(propertyName);
				if (property != null) {

					viewProperties.add(property);

				} else {

					nonGraphProperties.add(propertyName);
				}
			}

			SchemaView viewNode = app.nodeQuery(SchemaView.class)
				.and(SchemaView.schemaNode, schemaNode)
				.and(SchemaView.name, view.getKey())
				.getFirst();

			if (viewNode == null) {

				viewNode = app.create(SchemaView.class,
					new NodeAttribute<>(SchemaView.schemaNode, schemaNode),
					new NodeAttribute<>(SchemaView.name, view.getKey())
				);
			}

			final PropertyMap updateProperties = new PropertyMap();

			updateProperties.put(SchemaView.schemaProperties, viewProperties);
			updateProperties.put(SchemaView.nonGraphProperties, StringUtils.join(nonGraphProperties, ", "));

			if (viewOrder.containsKey(view.getKey())) {
				updateProperties.put(SchemaView.sortOrder, viewOrder.get(view.getKey()));
			}

			// update properties of existing or new schema view node
			viewNode.setProperties(SecurityContext.getSuperUserInstance(), updateProperties);
		}

		for (final StructrMethodDefinition method : methods) {

			final SchemaMethod schemaMethod = method.createDatabaseSchema(app, schemaNode);
			if (schemaMethod != null) {

				schemaMethods.put(schemaMethod.getName(), schemaMethod);
			}
		}

		// extends
		if (baseTypeReference != null) {

			final Object def = root.resolveURI(baseTypeReference);

			if (def != null && def instanceof JsonType) {

				final JsonType jsonType     = (JsonType)def;
				final String superclassName = "org.structr.dynamic." + jsonType.getName();

				nodeProperties.put(SchemaNode.extendsClass, superclassName);

			} else {

				final Class superclass = StructrApp.resolveSchemaId(baseTypeReference);
				if (superclass != null) {

					if (superclass.isInterface()) {

						nodeProperties.put(SchemaNode.implementsInterfaces, superclass.getName());

					} else {

						nodeProperties.put(SchemaNode.extendsClass, superclass.getName());
					}

				} else if ("https://structr.org/v1.1/definitions/FileBase".equals(baseTypeReference.toString())) {

					// FileBase doesn't exist any more, but we need to support it for some time..
					nodeProperties.put(SchemaNode.implementsInterfaces, "org.structr.web.entity.File");

				} else if (!StructrApp.getSchemaBaseURI().relativize(baseTypeReference).isAbsolute()) {

					// resolve internal type referenced in special URI
					final URI base          = StructrApp.getSchemaBaseURI().resolve("definitions/");
					final URI type          = base.relativize(baseTypeReference);
					final String typeName   = type.getPath();
					final String parameters = type.getQuery();

					if (StringUtils.isNotBlank(typeName)) {

						final Class internal  = StructrApp.getConfiguration().getNodeEntityClass(typeName);
						if (internal != null) {

							nodeProperties.put(SchemaNode.extendsClass, getParameterizedType(internal, parameters));
						}
					}
				}
			}
		}

		// implements
		if (!implementedInterfaces.isEmpty()) {

			final Set<String> interfaces = new LinkedHashSet<>();

			for (final URI implementedInterface : implementedInterfaces) {

				if (!isBuiltinType && implementedInterface.toString().equals("https://structr.org/v1.1/definitions/" + getName())) {
					isBuiltinType = true;
					nodeProperties.put(SchemaNode.isBuiltinType, isBuiltinType);
				}

				final Object def = root.resolveURI(implementedInterface);

				if (def != null && def instanceof JsonType) {

					final JsonType jsonType     = (JsonType)def;
					final String superclassName = "org.structr.dynamic." + jsonType.getName();

					if (jsonType.isInterface()) {

						interfaces.add(superclassName);

					} else {

						nodeProperties.put(SchemaNode.extendsClass, superclassName);
					}

				} else {

					final Class superclass = StructrApp.resolveSchemaId(implementedInterface);
					if (superclass != null) {

						interfaces.add(superclass.getName());

					} else {

						logger.warn("Unable to resolve built-in type {} against Structr schema", implementedInterface);

						SchemaService.blacklist(name);

						StructrApp.resolveSchemaId(implementedInterface);
					}
				}
			}

			nodeProperties.put(SchemaNode.implementsInterfaces, StringUtils.join(interfaces, ", "));
		}

		schemaNode.setProperties(SecurityContext.getSuperUserInstance(), nodeProperties);

		return schemaNode;
	}

	T getSchemaNode() {
		return schemaNode;
	}

	void setSchemaNode(final T schemaNode) {
		this.schemaNode = schemaNode;
	}

	Map<String, Set<String>> getViews() {
		return views;
	}

	Map<String, String> getViewOrder() {
		return viewOrder;
	}

	public boolean isBuiltinType() {
		return isBuiltinType;
	}

	public void setIsBuiltinType() {
		this.isBuiltinType = true;
	}

	void initializeReferenceProperties() {

		for (final StructrPropertyDefinition property : properties) {
			property.initializeReferences();
		}
	}

	// ----- static methods -----
	static StructrTypeDefinition deserialize(final StructrSchemaDefinition root, final String name, final Map<String, Object> source) {

		final Map<String, StructrPropertyDefinition> deserializedProperties = new TreeMap<>();
		final Map<String, StructrMethodDefinition> deserializedMethods      = new TreeMap<>();
		final StructrTypeDefinition typeDefinition                          = StructrTypeDefinition.determineType(root, name, source);
		final Map<String, Object> properties                                = (Map)source.get(JsonSchema.KEY_PROPERTIES);
		final List<String> requiredPropertyNames                            = (List)source.get(JsonSchema.KEY_REQUIRED);
		final Map<String, Object> views                                     = (Map)source.get(JsonSchema.KEY_VIEWS);
		final Map<String, String> viewOrder                                 = (Map)source.get(JsonSchema.KEY_VIEW_ORDER);
		final Map<String, Object> methods                                   = (Map)source.get(JsonSchema.KEY_METHODS);

		if (properties != null) {

			for (final Entry<String, Object> entry : properties.entrySet()) {

				final String propertyName = entry.getKey();
				final Object value        = entry.getValue();

				if (value instanceof Map) {

					final StructrPropertyDefinition property = StructrPropertyDefinition.deserialize(typeDefinition, propertyName, (Map)value);
					if (property != null) {

						deserializedProperties.put(property.getName(), property);
						typeDefinition.getProperties().add(property);
					}

				} else {

					throw new IllegalStateException("Invalid JSON property definition for property " + propertyName + ", expected object.");
				}
			}
		}

		if (requiredPropertyNames != null) {

			for (final String requiredPropertyName : requiredPropertyNames) {

				// set required properties
				final StructrPropertyDefinition property = deserializedProperties.get(requiredPropertyName);
				if (property != null) {

					property.setRequired(true);

				} else {

					throw new IllegalStateException("Required property " + requiredPropertyName + " not defined for type " + typeDefinition.getName() + ".");
				}
			}
		}

		if (views != null) {

			for (final Entry<String, Object> entry : views.entrySet()) {

				final String viewName = entry.getKey();
				final Object value    = entry.getValue();

				if (value instanceof List) {

					final Set<String> viewProperties = new TreeSet<>((List)value);
					typeDefinition.getViews().put(viewName, viewProperties);

				} else {

					throw new IllegalStateException("View definition " + viewName + " must be of type array.");
				}
			}
		}

		if (viewOrder != null) {

			typeDefinition.getViewOrder().putAll(viewOrder);
		}

		if (methods != null) {

			for (final Entry<String, Object> entry : methods.entrySet()) {

				final String methodName = entry.getKey();
				final Object value      = entry.getValue();

				if (value instanceof Map) {

					final StructrMethodDefinition method = StructrMethodDefinition.deserialize(typeDefinition, methodName, (Map)value);
					if (method != null) {

						deserializedMethods.put(method.getName(), method);
						typeDefinition.getMethods().add(method);
					}

				} else if (value instanceof Collection) {

					// more than on method with the same name exists..
					final Collection<Map<String, Object>> methodList = (Collection)value;

					for (final Map<String, Object> map : methodList) {

						final StructrMethodDefinition method = StructrMethodDefinition.deserialize(typeDefinition, methodName, map);
						if (method != null) {

							deserializedMethods.put(method.getName(), method);
							typeDefinition.getMethods().add(method);
						}
					}

				} else {

					throw new IllegalStateException("Method definition " + methodName + " must be of type string or map.");
				}
			}
		}

		final Object isAbstractValue = source.get(JsonSchema.KEY_IS_ABSTRACT);
		if (isAbstractValue != null && Boolean.TRUE.equals(isAbstractValue)) {

			typeDefinition.setIsAbstract();
		}

		final Object isInterfaceValue = source.get(JsonSchema.KEY_IS_INTERFACE);
		if (isInterfaceValue != null && Boolean.TRUE.equals(isInterfaceValue)) {

			typeDefinition.setIsInterface();
		}

		final Object isBuiltinType = source.get(JsonSchema.KEY_IS_BUILTIN_TYPE);
		if (isBuiltinType != null && Boolean.TRUE.equals(isBuiltinType)) {

			typeDefinition.setIsBuiltinType();
		}

		final Object categoryValue = source.get(JsonSchema.KEY_CATEGORY);
		if (categoryValue != null) {

			typeDefinition.setCategory(categoryValue.toString());
		}

		return typeDefinition;
	}

	static StructrTypeDefinition deserialize(final Map<String, SchemaNode> schemaNodes, final StructrSchemaDefinition root, final SchemaNode schemaNode) {

		final StructrNodeTypeDefinition def = new StructrNodeTypeDefinition(root, schemaNode.getClassName());
		def.deserialize(schemaNodes, schemaNode);

		return def;
	}

	static StructrTypeDefinition deserialize(final Map<String, SchemaNode> schemaNodes, final StructrSchemaDefinition root, final SchemaRelationshipNode schemaRelationship) {

		final StructrRelationshipTypeDefinition def = new StructrRelationshipTypeDefinition(root, schemaRelationship.getClassName());
		def.deserialize(schemaNodes, schemaRelationship);

		return def;
	}

	// ----- protected methods -----
	protected SchemaNode resolveSchemaNode(final Map<String, SchemaNode> schemaNodes, final App app, final URI uri) throws FrameworkException {

		// find schema nodes for the given source and target nodes
		final Object source = root.resolveURI(uri);
		if (source != null && source instanceof StructrTypeDefinition) {

			return (SchemaNode)((StructrTypeDefinition)source).getSchemaNode();

		} else {

			if (uri.isAbsolute()) {

				final Class type = StructrApp.resolveSchemaId(uri);
				if (type != null) {

					return schemaNodes.get(type.getSimpleName());
				}
			}
		}

		return null;
	}

	// ----- private methods -----
	private static StructrTypeDefinition determineType(final StructrSchemaDefinition root, final String name, final Map<String, Object> source) {

		if (source.containsKey(JsonSchema.KEY_RELATIONSHIP)) {

			final StructrRelationshipTypeDefinition def = new StructrRelationshipTypeDefinition(root, name);
			def.deserialize(source);

			return def;

		} else {

			final StructrNodeTypeDefinition def = new StructrNodeTypeDefinition(root, name);
			def.deserialize(source);

			return def;
		}
	}

	private void addPropertyNameToViews(final String name, final String... views) {

		for (final String viewName : views) {

			Set<String> view = this.views.get(viewName);
			if (view == null) {

				view = new TreeSet<>();
				this.views.put(viewName, view);
			}

			// add property name to view
			view.add(name);
		}
	}

	private String getParameterizedType(final Class type, final String queryString) {

		final StringBuilder buf           = new StringBuilder();
		final List<String> typeParameters = new LinkedList<>();

		buf.append(type.getName());

		if (queryString != null) {

			final Map<String, String> params = UrlUtils.parseQueryString(queryString);
			final String types               = params.get("typeparameters");

			if (types != null) {

				final String[] parameterTypes = types.split("[, ]+");
				for (final String parameterType : parameterTypes) {

					final String trimmed = parameterType.trim();
					if (StringUtils.isNotBlank(trimmed)) {

						typeParameters.add(trimmed);
					}
				}

				buf.append("<");
				buf.append(StringUtils.join(typeParameters, ", "));
				buf.append(">");
			}
		}

		return buf.toString();

	}

	private String resolveParameterizedType(final String parameterizedType) {

		// input is something like "org.structr.core.entity.LinkedTreeNodeImpl<org.structr.web.entity.AbstractFile>"
		// output is LinkedTreeNodeImpl?typeParameters=org.structr.web.entity.AbstractFile

		if (parameterizedType.contains("<") && parameterizedType.contains(">")) {

			final int startOfTypeParam = parameterizedType.indexOf("<");
			final String typeParameter = parameterizedType.substring(startOfTypeParam);
			final String baseType      = parameterizedType.substring(0, startOfTypeParam);

			return baseType + "?typeParameters=" + typeParameter.substring(1, typeParameter.length() - 1);
		}

		return parameterizedType.substring(parameterizedType.lastIndexOf(".") + 1);
	}
}
