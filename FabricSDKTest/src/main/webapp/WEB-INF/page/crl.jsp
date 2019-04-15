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

<form action="cert" method="post">
    <input type="submit" value="cert">
</form>
<div style="text-align: center">
    <form action="set_car_crl" method="post"  >
        汽车公钥: <textarea name="car_key" style="vertical-align: middle" ></textarea> <br/>
        恶意标签: <textarea name="car_bad_flag" style="vertical-align: middle" ></textarea> <br/>
        <input type="submit" value="上传恶意车辆">
    </form>
    交易信息为：
    <pre style="margin: 0 auto; text-align: left;width: max-content; color: red">
        ${logger}
    </pre>
    背书时间:<input type="text" name="send_proposal_time" value=${proposal_time}>
    交易生成区块及上链时间:<input type="text" name="finish_time" value=${block_time}>
</div>
<br><br><br><br><br><br>
<div style="text-align: center">
    <form action="get_car_crl" method="post">
        汽车公钥: <textarea name="car_key" style="vertical-align: middle" ></textarea> <br/>
        <input type="submit" value="查询车辆是否恶意">
    </form>
    <pre style="margin: 0 auto; text-align: left;width: max-content">${key}</pre>
    恶意车辆?<br>
    <pre style="margin: 0 auto; text-align: left;width: max-content; color: red">
       <textarea name="car_crl_value">${value}</textarea>
    </pre>
</div>




</body>
</html>

