package org.wso2.deployment.monitor.api;

/**
 * A bean to represent a Login for a Host
 */
public class HostBean {

    private String hostName;

    private boolean isTaskSuccess;

    private int nodeIndex;

    private String errorMsg;


    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public boolean isTaskSuccess() {
        return isTaskSuccess;
    }

    public void setTaskSuccess(boolean taskSuccess) {
        isTaskSuccess = taskSuccess;
    }

    public int getNodeIndex() {
        return nodeIndex;
    }

    public void setNodeIndex(int nodeIndex) {
        this.nodeIndex = nodeIndex;
    }

    public String getErrorMsg() {
        return errorMsg;
    }

    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }
}
