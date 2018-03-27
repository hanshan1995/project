<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <%-- 样式 --%>
    <%@include file="/WEB-INF/views/static/head.jsp" %>
    <title>后台管理-修改密码</title>
</head>
<body>
<%-- 导航条 --%>
<%@include file="/WEB-INF/views/static/nav.jsp" %>

<%-- 内容 --%>
<div class="container">
    <div class="row">
        <div class="col-md-2">
            <ul class="nav nav-pills nav-stacked">
                <li><a href="${contextPath}/profile">个人资料</a></li>
                <li class="active"><a href="javascript:void(0)">修改密码</a></li>
                <li><a href="#">板块管理</a></li>
                <li><a href="#">用户管理</a></li>
                <li><a href="#">权限管理</a></li>
                <li><a href="#">角色管理</a></li>
            </ul>
        </div>
        <div class="col-md-10">
            <div class="tab-content tab-pane">
                <div class="panel panel-default">
                    <div class="panel-heading">修改密码</div>
                    <div class="panel-body">
                        <form class="form-horizontal" id="form-password">
                            <div class="form-group">
                                <label for="input-password" class="col-md-2 control-label">原密码</label>
                                <div class="col-md-4">
                                    <input class="form-control" id="input-password" placeholder="原密码"/>
                                </div>
                            </div>
                            <div class="form-group">
                                <label for="input-password-new" class="col-md-2 control-label">新密码</label>
                                <div class="col-md-4">
                                    <input class="form-control" id="input-password-new" placeholder="新密码"/>
                                </div>
                            </div>
                            <div class="form-group">
                                <label for="input-password-confirm" class="col-md-2 control-label">确认密码</label>
                                <div class="col-md-4">
                                    <input class="form-control" id="input-password-confirm" placeholder="确认密码"/>
                                </div>
                            </div>
                            <div class="form-group">
                                <div class="col-md-offset-2 col-md-10">
                                    <button id="btn-submit" type="submit" class="btn btn-default">保存</button>
                                </div>
                            </div>
                        </form>
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>
<%-- 通用注脚 --%>
<%@include file="/WEB-INF/views/static/footer.jsp" %>
</body>
<%-- 通用脚本 --%>
<%@include file="/WEB-INF/views/static/script.jsp" %>

<script>
    $("#form-password").submit(function (e) {

        var oldPassword = $('#input-password').val();
        var newPassword = $('#input-password-new').val();
        var confirmPassword = $('#input-password-confirm').val();

        if(newPassword != confirmPassword){
            swal({
                type: 'warning',
                title: '警告',
                text: '确认密码不一致, 请重新输入'
            });
            e.preventDefault();
            return;
        }

        $.ajax({
            type: "POST",
            url: ctx+"/api/modify/password",
            data: { oldPassword: oldPassword, newPassword:newPassword },
            success: function (json) {
                if(!json.result){
                    swal({
                        type: 'warning',
                        title: '警告',
                        text: json.msg
                    });
                    return;
                }

                swal({
                    type: 'success',
                    title: '成功',
                    text: json.msg
                });
                setTimeout(function () {
                    location.reload();
                }, 5000);
            }
        });
        e.preventDefault(); // avoid to execute the actual submit of the form.
    });
</script>
</html>