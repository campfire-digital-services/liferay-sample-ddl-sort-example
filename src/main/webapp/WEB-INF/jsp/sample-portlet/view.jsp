<%@ include file="init.jsp"%>

<div>Record Set Id: ${recordSetId}</div>

<portlet:actionURL name="add" var="addUrl" />
<portlet:actionURL name="sort" var="sortUrl" />
<portlet:actionURL name="create" var="createUrl" />
<c:if test="${recordSetId == -1}">

	<aui:a href="${createUrl }">Create sample table</aui:a>

</c:if>

<c:if test="${recordSetId > 0}">
	<aui:form action="${addUrl }">
		<aui:input name="rows" type="text"/>
		
		<aui:button type="submit" value="Create rows"></aui:button>
	</aui:form>
	
	<aui:form action="${sortUrl }">
		<aui:input name="columnName" type="text"/>
		
		<aui:button type="submit" value="sort"></aui:button>
	</aui:form>
	
	<div>Total rows: ${recordTable.rowMap().size() }</div>
	<table style="width:100%;">
	  <thead>
    	<c:forEach items="${recordTable.columnKeySet()}" var="colHeader">
    	   <th>${colHeader}</th>
    	</c:forEach>
	  </thead>
	<c:forEach items="${displayRows}" var="row">
	   <tr>
	       <c:forEach items="${recordTable.row(row).values()}" var="value">
	           <td>${value}</td>
	       </c:forEach>
	   </tr>
	</c:forEach>
	</table>
	
</c:if>