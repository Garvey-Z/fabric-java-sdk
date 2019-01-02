<%@ page contentType="text/html;charset=UTF-8" language="java" %>


<html>
<style>
    pre {
        white-space: pre-wrap; /*css-3*/
        white-space: -moz-pre-wrap; /*Mozilla,since1999*/
        white-space: -o-pre-wrap; /*Opera7*/
        word-wrap: break-word; /*InternetExplorer5.5+*/
    }
</style>
<body>

<div style="text-align: center">
    <form action="set_Car_CA" method="post"  >
        汽车公钥: <textarea name="car_key" style="vertical-align: middle" ></textarea> <br/>
        汽车证书: <textarea name="car_ca" style="vertical-align: middle" ></textarea> <br/>
        <input type="submit" value="上传证书">
    </form>
   交易信息为：
    <pre style="margin: 0 auto; text-align: left;width: max-content; color: red">
         ${logger}
    </pre>
</div>
<br><br><br><br><br><br>
<div style="text-align: center">
    <form action="get_Car_CA" method="post">
            汽车公钥: <textarea name="car_key" style="vertical-align: middle" ></textarea> <br/>
        <input type="submit" value="获取证书">
    </form>
    <pre style="margin: 0 auto; text-align: left;width: max-content">${key}</pre>
    的证书包为: <br>
    <pre style="margin: 0 auto; text-align: left;width: max-content; color: red">${value}</pre>
</div>
<%--<br><br><br>--%>
<%--<div style="text-align: center">--%>
    <%--<form action="getBlockByNumber" method="post">--%>
        <%--区块: <textarea name="block_number" style="vertical-align: middle" ></textarea> <br/>--%>
        <%--<input type="submit" value="获取区块">--%>
    <%--</form>--%>
    <%--区块${block_number}的信息为: <br>--%>
    <%--<pre style="margin: 0 auto; text-align: left;width: max-content; color: red">--%>
        <%--${block_value}--%>
    <%--</pre>--%>
<%--</div>--%>

</body>
</html>

