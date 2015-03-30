<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<div class="row container-fluid">

  <div class="col-sm-10 col-md-6 col-md-offset-2 col-lg-4 col-lg-offset-3">
    <div class="panel panel-default">
      <div class="panel-heading">
        <span class="panel-title">Upload a new description</span>
      </div>
      <div class="panel-body row">
        <c:choose>
          <c:when test="${ not empty storeList }">

            <form class="col-sm-8 col-sm-offset-1" method="post" action="${ pageContext.request.contextPath }/offerings/register">
              <div class="form-field">
                <label class="text-plain">Store *</label>
                <select class="form-control" name="storeName">
                  <c:forEach var="store" items="${ storeList }">
                    <c:choose>
                      <c:when test="${ not empty field_storeName and field_storeName == store.name }">
                        <option value="${ store.name }" selected>${ store.displayName }</option>
                      </c:when>
                      <c:otherwise>
                        <option value="${ store.name }">${ store.displayName }</option>
                      </c:otherwise>
                    </c:choose>
                  </c:forEach>

                </select>
                <c:if test="${ not empty form_error and form_error.fieldName == 'storeName' }">
                  <div class="form-field-error">${ form_error.fieldError }</div>
                </c:if>
              </div>

              <div class="form-field">
                <label class="text-plain">Description name *</label>
                <input class="form-control" type="text" name="displayName" value="${ field_displayName }" />

                <c:if test="${ not empty form_error and form_error.fieldName == 'displayName' }">
                  <div class="form-field-error">${ form_error.fieldError }</div>
                </c:if>
              </div>

              <div class="form-field">
                <label class="text-plain">Description URL * <a href="http://linked-usdl.org/"><span class="fa fa-info-circle"></span></a></label>
                <input class="form-control" type="text" name="url" value="${ field_url }" />

                <c:if test="${ not empty form_error and form_error.fieldName == 'url' }">
                  <div class="form-field-error">${ form_error.fieldError }</div>
                </c:if>
              </div>

              <p class="text-plain text-default">* Required fields</p>
              <div class="form-options">
                <button type="submit" class="btn btn-warning btn-sm-10 btn-md-5">Upload</button>
              </div>
            </form>

          </c:when>
          <c:otherwise>
            <div class="alert alert-info">
              <span class="fa fa-info-circle"></span> To upload offerings, it is necessary to exist at least one store. Go to <a class="alert-link" href="${ pageContext.request.contextPath }/stores/register">register a store</a>.
            </div>
          </c:otherwise>
        </c:choose>
      </div>
    </div>
  </div>

</div><!-- /.container-fluid -->
