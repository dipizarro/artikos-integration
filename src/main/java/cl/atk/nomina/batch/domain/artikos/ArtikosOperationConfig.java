package cl.atk.nomina.batch.domain.artikos;

public class ArtikosOperationConfig {

    private String token;
    private String msgCode;
    private String msgFromAddress;
    private String msgCodFromAddress;
    private String msgToAddress;
    private String msgCodSis;
    private String msgCodExterno;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getMsgCode() {
        return msgCode;
    }

    public void setMsgCode(String msgCode) {
        this.msgCode = msgCode;
    }

    public String getMsgFromAddress() {
        return msgFromAddress;
    }

    public void setMsgFromAddress(String msgFromAddress) {
        this.msgFromAddress = msgFromAddress;
    }

    public String getMsgCodFromAddress() {
        return msgCodFromAddress;
    }

    public void setMsgCodFromAddress(String msgCodFromAddress) {
        this.msgCodFromAddress = msgCodFromAddress;
    }

    public String getMsgToAddress() {
        return msgToAddress;
    }

    public void setMsgToAddress(String msgToAddress) {
        this.msgToAddress = msgToAddress;
    }

    public String getMsgCodSis() {
        return msgCodSis;
    }

    public void setMsgCodSis(String msgCodSis) {
        this.msgCodSis = msgCodSis;
    }

    public String getMsgCodExterno() {
        return msgCodExterno;
    }

    public void setMsgCodExterno(String msgCodExterno) {
        this.msgCodExterno = msgCodExterno;
    }
}
