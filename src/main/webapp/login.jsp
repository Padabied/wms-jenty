<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>WMS-Jenty: Авторизация</title>
    <link rel="icon" type="image/svg+xml" href="${pageContext.request.contextPath}/images/logo.svg">
    <style>
        body {
            margin: 0;
            padding: 0;
            min-height: 100vh;
            display: flex;
            justify-content: center;
            align-items: center;
            background: #ffffff;
        }
        .container {
            max-width: 350px;
            background: #f8f9fd;
            background: linear-gradient(
                    0deg,
                    rgb(255, 255, 255) 0%,
                    rgb(245, 250, 246) 100%
            );
            border-radius: 40px;
            padding: 25px 35px;
            border: 5px solid rgb(255, 255, 255);
            box-shadow: rgba(57, 115, 67, 0.8784313725) 0px 30px 30px -20px;
            margin: 20px;
        }

        .heading {
            text-align: center;
            font-weight: 900;
            font-size: 30px;
            color: #22522a;
        }

        .form {
            margin-top: 20px;
        }

        .form .input {
            width: 88%;
            background: white;
            border: none;
            padding: 15px 20px;
            border-radius: 20px;
            margin-top: 15px;
            box-shadow: #397343 0px 10px 10px -5px;
            border-inline: 2px solid transparent;
        }

        .form .input::-moz-placeholder {
            color: rgb(170, 170, 170);
        }

        .form .input::placeholder {
            color: rgb(170, 170, 170);
        }

        .form .input:focus {
            outline: none;
            border-inline: 2px solid #3f4f3f;
        }

        .form .login-button {
            display: block;
            width: 40%;
            font-weight: bold;
            background: linear-gradient(
                    45deg,
                    rgb(57, 115, 67) 0%,
                    rgb(19, 20, 19) 100%
            );
            color: white;
            padding-block: 15px;
            margin: 20px auto;
            border-radius: 20px;
            box-shadow: rgba(57, 115, 67, 0.8784313725) 0px 20px 10px -15px;
            border: none;
            transition: all 0.2s ease-in-out;
        }

        .form .login-button:hover {
            transform: scale(1.03);
            box-shadow: rgba(57, 115, 67, 0.8784313725) 0px 23px 10px -20px;
        }

        .form .login-button:active {
            transform: scale(0.95);
            box-shadow: rgba(57, 115, 67, 0.8784313725) 0px 15px 10px -10px;
        }

        .logo {
            position: fixed;
            bottom: 40px;
            right: 40px;
            width: 200px;        /* размер по желанию */
            height: auto;
        }
    </style>
</head>
<body>
<div class="container">
    <%
        String error = (String) request.getAttribute("error");
        if (error != null) {
    %>
    <div style="color: #ffffff; text-align: center; margin-bottom: 15px; padding: 10px; background: #164912; border-radius: 10px;">
        <%= error %>
    </div>
    <%
        }
    %>
    <div class="heading">WMS-Jenty</div>
    <form class="form" action="${pageContext.request.contextPath}/login" method="post" autocomplete="off">
        <input
                placeholder="Логин"
                id="login"
                name="login"
                type="login"
                class="input"
                required=""
        />
        <input
                placeholder="Пароль"
                id="password"
                name="password"
                type="password"
                class="input"
                required=""
        />
        <input value="Вход" type="submit" class="login-button" />
    </form>
</div>
<img src="${pageContext.request.contextPath}/images/logo.svg"
     class="logo"
     alt="decorative">
</body>
</html>