class KeyMap {
    private int initKeyCode;
    private int finalKeyCode;

    KeyMap(int initKeyCode, int finalKeyCode){ 
        this.initKeyCode = initKeyCode;
        this.finalKeyCode = finalKeyCode;
    }
    KeyMap(int initKeyCode){
        this.initKeyCode = initKeyCode;
        this.finalKeyCode = -1;
    }
    KeyMap(){
        this.initKeyCode = -1;
        this.initKeyCode = -1;
    }
    public void setInitKeyCode(int initKeyCode){
        this.initKeyCode = initKeyCode;
    }
    public void setFinalKeyCode(int finalKeyCode){
        this.finalKeyCode = finalKeyCode;
    }
    public int getInitKeyCode(){
        return initKeyCode;
    }
    public int getFinalKeyCode(){
        return finalKeyCode;
    }
    public void clear(){
        this.initKeyCode = -1;
        this.finalKeyCode = -1;
    }
}