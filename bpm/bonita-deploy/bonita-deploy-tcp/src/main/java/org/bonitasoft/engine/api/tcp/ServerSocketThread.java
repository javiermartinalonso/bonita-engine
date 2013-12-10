package org.bonitasoft.engine.api.tcp;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;

import org.bonitasoft.engine.api.internal.ServerAPI;
import org.bonitasoft.engine.api.internal.ServerWrappedException;

public class ServerSocketThread extends Thread {

    private final ServerSocket serverSocket;

    private final ServerAPI serverApi;

    public ServerSocketThread(final String name, final ServerAPI serverApi, final int port) throws IOException {
        super(name);
        this.serverApi = serverApi;
        // System.out.println(this.getClass().getSimpleName() + " - " + this.getName() + "starting...");
        serverSocket = new ServerSocket(port);
        // System.out.println(this.getClass().getSimpleName() + " - " + this.getName() + "serverSocket build: " + serverSocket);

    }

    private Object invokeMethod(final MethodCall methodCall) throws ServerWrappedException, RemoteException {
        final Map<String, Serializable> options = methodCall.getOptions();
        final String apiInterfaceName = methodCall.getApiInterfaceName();
        final String methodName = methodCall.getMethodName();
        final List<String> classNameParameters = methodCall.getClassNameParameters();
        final Object[] parametersValues = methodCall.getParametersValues();
        // System.out.println(this.getClass().getSimpleName() + " - " + this.getName() + " - invoking: with parameters: "
        // + ", options: " + options
        // + ", apiInterfaceName: " + apiInterfaceName
        // + ", methodName: " + methodName
        // + ", classNameParameters: " + classNameParameters
        // + ", parametersValues: " + parametersValues
        // + "...");
        try {
            return this.invokeMethod(options, apiInterfaceName, methodName, classNameParameters, parametersValues);
        } catch (ServerWrappedException e) {
            // System.out.println(this.getClass().getSimpleName() + " - " + this.getName() + "got an exception during the invokeMethod: " + e.getClass() + ": "
            // + e.getMessage());
            e.printStackTrace();
            return e;
        }
    }

    @Override
    public void run() {
        while (true) {
            try {
                final Socket clientSocket = serverSocket.accept();
                // System.out.println(this.getClass().getSimpleName() + " - " + this.getName() + "accepting data on serverSocket, clientSocket: " +
                // clientSocket);
                // System.out.println(this.getClass().getSimpleName() + " - " + this.getName() + "starting a new loop...");
                final ObjectInputStream ois = new ObjectInputStream(clientSocket.getInputStream());
                ObjectOutputStream oos = null;
                try {
                    final MethodCall methodCall = (MethodCall) ois.readObject();
                    // System.out.println(this.getClass().getSimpleName() + " - " + this.getName() + "received methodCall: " + methodCall);
                    final Object callResult = invokeMethod(methodCall);
                    // System.out.println(this.getClass().getSimpleName() + " - " + this.getName() + "callResult: " + callResult);
                    oos = new ObjectOutputStream(clientSocket.getOutputStream());
                    oos.writeObject(callResult);
                    // System.out.println(this.getClass().getSimpleName() + " - " + this.getName() + "flushing callResult: " + callResult);
                    oos.flush();
                } catch (Throwable t) {
                    t.printStackTrace();
                } finally {
                    if (ois != null) {
                        ois.close();
                    }
                    if (oos != null) {
                        oos.close();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public Object invokeMethod(final Map<String, Serializable> options, final String apiInterfaceName, final String methodName,
            final List<String> classNameParameters, final Object[] parametersValues) throws ServerWrappedException, RemoteException {
        return serverApi.invokeMethod(options, apiInterfaceName, methodName, classNameParameters, parametersValues);
    }
}
