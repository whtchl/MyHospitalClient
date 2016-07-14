package client.jdjz.com.service;

public interface IConnectionStatusCallback {
	public void connectionStatusChanged(int connectedState, String reason);
}
