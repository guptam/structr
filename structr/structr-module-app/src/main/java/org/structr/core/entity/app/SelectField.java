/*
 *  Copyright (C) 2011 Axel Morgner, structr <structr@structr.org>
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



package org.structr.core.entity.app;

import org.apache.commons.lang.StringUtils;
<<<<<<< HEAD
import org.structr.common.RenderMode;
import org.structr.common.RequestHelper;
=======

import org.structr.common.CurrentRequest;
import org.structr.common.CurrentSession;
import org.structr.common.PropertyView;
import org.structr.common.RenderMode;
import org.structr.common.SessionValue;
>>>>>>> 0f55394c125ecab035924262c7b0c1fb27248885
import org.structr.common.renderer.ExternalTemplateRenderer;
import org.structr.core.EntityContext;
import org.structr.core.NodeRenderer;

//~--- JDK imports ------------------------------------------------------------

import java.util.Map;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;

//~--- classes ----------------------------------------------------------------

/**
 * Render a select field.
 *
 * When connected with a node via a DATA relationship, all direct children of
 * this node are treated as options list.
 *
 * @author Axel Morgner
 */
public class SelectField extends FormField implements InteractiveNode {

	private static final Logger logger = Logger.getLogger(SelectField.class.getName());

	//~--- static initializers --------------------------------------------

	static {

		EntityContext.registerPropertySet(PasswordField.class,
						  PropertyView.All,
						  Key.values());
	}

	//~--- fields ---------------------------------------------------------

	protected SessionValue<Object> errorSessionValue = null;
	private String mappedName                        = null;
	protected SessionValue<String> sessionValue      = null;

	//~--- methods --------------------------------------------------------

	@Override
	public void initializeRenderers(Map<RenderMode, NodeRenderer> renderers) {

		renderers.put(RenderMode.Default,
			      new ExternalTemplateRenderer(false));
	}

	//~--- get methods ----------------------------------------------------

	@Override
	public String getIconSrc() {
		return "/images/textfield.png";
	}

	// ----- interface InteractiveNode -----
	@Override
<<<<<<< HEAD
	public String getValue(HttpServletRequest request)
	{
=======
	public String getValue() {

		HttpServletRequest request  = CurrentRequest.getRequest();
>>>>>>> 0f55394c125ecab035924262c7b0c1fb27248885
		String valueFromLastRequest = null;
		String name                 = getName();
		String ret                  = null;

		// only return value from last request if we were redirected before
<<<<<<< HEAD
		if(RequestHelper.isRedirected(request))
		{
			valueFromLastRequest = getLastValue().get(request);
=======
		if (CurrentSession.isRedirected()) {
			valueFromLastRequest = getLastValue().get();
		} else {
>>>>>>> 0f55394c125ecab035924262c7b0c1fb27248885

			// otherwise, clear value in session
			getLastValue().set(request, null);
		}

		if (request == null) {
			return valueFromLastRequest;
		}

		if (request != null) {

			ret = request.getParameter(name);

			if (ret != null) {

				// Parameter is there
				if (ret.length() == 0) {

					// Empty value
					return null;
				} else {

					// store value in session, in case we get a redirect afterwards
<<<<<<< HEAD
					getLastValue().set(request, ret);
=======
					getLastValue().set(ret);

>>>>>>> 0f55394c125ecab035924262c7b0c1fb27248885
					return ret;
				}
			} else {

				// Parameter is not in request
				return valueFromLastRequest;
			}
		}

		return null;
	}

	@Override
<<<<<<< HEAD
	public String getStringValue(HttpServletRequest request)
	{
		Object value = getValue(request);
		return (value != null ? value.toString() : null);
=======
	public String getStringValue() {

		Object value = getValue();

		return ((value != null)
			? value.toString()
			: null);
>>>>>>> 0f55394c125ecab035924262c7b0c1fb27248885
	}

	@Override
	public Class getParameterType() {
		return (String.class);
	}

	@Override
	public String getMappedName() {

		if (StringUtils.isNotBlank(mappedName)) {
			return (mappedName);
		}

		return (getName());
	}

	@Override
<<<<<<< HEAD
	public void setErrorValue(HttpServletRequest request, Object errorValue)
	{
		getErrorMessageValue().set(request, errorValue);
	}

	@Override
	public Object getErrorValue(HttpServletRequest request)
	{
		return (getErrorMessageValue().get(request));
	}

	@Override
	public String getErrorMessage(HttpServletRequest request)
	{
		Object errorValue = getErrorValue(request);
		if(errorValue != null)
		{
=======
	public Object getErrorValue() {
		return (getErrorMessageValue().get());
	}

	@Override
	public String getErrorMessage() {

		Object errorValue = getErrorValue();

		if (errorValue != null) {
>>>>>>> 0f55394c125ecab035924262c7b0c1fb27248885
			return (errorValue.toString());
		}

		return (null);
	}

	// ----- private methods -----
	private SessionValue<Object> getErrorMessageValue() {

		if (errorSessionValue == null) {
			errorSessionValue = new SessionValue<Object>(createUniqueIdentifier("errorMessage"));
		}

		return (errorSessionValue);
	}

	private SessionValue<String> getLastValue() {

		if (sessionValue == null) {
			sessionValue = new SessionValue<String>(createUniqueIdentifier("lastValue"));
		}

		return (sessionValue);
	}

	//~--- set methods ----------------------------------------------------

	@Override
	public void setMappedName(String mappedName) {
		this.mappedName = mappedName;
	}

	@Override
	public void setErrorValue(Object errorValue) {
		getErrorMessageValue().set(errorValue);
	}

	// apperently not used
//      private List<AbstractNode> getDataNodes(final User user) {
//
//          List<AbstractNode> dataNodes = new LinkedList<AbstractNode>();
//
//          List<StructrRelationship> dataRels = this.getRelationships(RelType.DATA, Direction.INCOMING);
//
//          for (StructrRelationship rel : dataRels) {
//
//              AbstractNode node = rel.getStartNode();
//              dataNodes.addAll(node.getDirectChildNodes());
//
//          }
//
//          return dataNodes;
//
//      }
}
