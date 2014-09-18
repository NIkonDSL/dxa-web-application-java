<!DOCTYPE html>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>
<jsp:useBean id="pageModel" type="com.sdl.tridion.referenceimpl.common.model.Page" scope="request"/>
<html>
<head>
    <title><c:out value="${pageModel.title}"/></title>
</head>
<body>
<c:forEach var="entry" items="${pageModel.regions}">
<p><c:out value="${entry.value.viewName}"/></p>
<jsp:include page="/region/${entry.value.viewName}"/>
</c:forEach>
</body>
</html>