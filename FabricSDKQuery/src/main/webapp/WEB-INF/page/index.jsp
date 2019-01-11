<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
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

<br><br><br>
<div style="text-align: center">

      <form action="index" method="post">
          <select id="peerName" name="peerName">
              <option value="NONE">请选择节点...</option>
              <option value="peer0.org1.example.com">peer0.org1.example.com</option>
              <option value="peer1.org1.example.com">peer1.org1.example.com</option>
              <option value="peer0.org2.example.com">peer0.org2.example.com</option>
              <option value="peer1.org2.example.com">peer1.org2.example.com</option>
          </select>
          <input type="submit" value="确定">
      </form>
    <br><br><br>
    ${PeerName}
        <br><br><br>
    <form action="get_Car_CA" method="post">
            汽车公钥: <textarea name="car_key" style="vertical-align: middle" ></textarea> <br/>
        <input type="submit" value="获取证书">
    </form>
    <pre style="margin: 0 auto; text-align: left;width: max-content">${key}</pre>
    的证书包为: <br>
    <pre style="margin: 0 auto; text-align: left;width: max-content; color: red">${value}</pre>
</div>

</body>
</html>

