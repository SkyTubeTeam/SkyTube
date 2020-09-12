package free.rm.skytube.businessobjects;

import android.util.Log;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

/**
 * This is an extension of the SSLSocketFactory which enables TLS 1.2 and 1.1.
 * Created for usage on Android 4.1-4.4 devices, which haven't enabled those by default.
 *
 * Based on TLSSocketFactoryCompat from NewPipe.
 */
public class TLSSocketFactory extends SSLSocketFactory {
    private static TLSSocketFactory instance = null;

    private SSLSocketFactory internalSSLSocketFactory;

    static TLSSocketFactory getInstance() throws NoSuchAlgorithmException, KeyManagementException {
        if (instance != null) {
            return instance;
        }
        return instance = new TLSSocketFactory();
    }

    TLSSocketFactory() throws KeyManagementException, NoSuchAlgorithmException {
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(null, null, null);
        internalSSLSocketFactory = context.getSocketFactory();
    }

    public static void setAsDefault() {
        try {
            HttpsURLConnection.setDefaultSSLSocketFactory(getInstance());
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            Log.e("TLSSocketFactory", "Setup failed: "+e.getMessage(), e);
        }
    }

    @Override
    public String[] getDefaultCipherSuites() {
        return internalSSLSocketFactory.getDefaultCipherSuites();
    }

    @Override
    public String[] getSupportedCipherSuites() {
        return internalSSLSocketFactory.getSupportedCipherSuites();
    }

    @Override
    public Socket createSocket() throws IOException {
        return enableTLSOnSocket(internalSSLSocketFactory.createSocket());
    }

    @Override
    public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException {
        return enableTLSOnSocket(internalSSLSocketFactory.createSocket(s, host, port, autoClose));
    }

    @Override
    public Socket createSocket(String host, int port) throws IOException, UnknownHostException {
        return enableTLSOnSocket(internalSSLSocketFactory.createSocket(host, port));
    }

    @Override
    public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException, UnknownHostException {
        return enableTLSOnSocket(internalSSLSocketFactory.createSocket(host, port, localHost, localPort));
    }

    @Override
    public Socket createSocket(InetAddress host, int port) throws IOException {
        return enableTLSOnSocket(internalSSLSocketFactory.createSocket(host, port));
    }

    @Override
    public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
        return enableTLSOnSocket(internalSSLSocketFactory.createSocket(address, port, localAddress, localPort));
    }

    private Socket enableTLSOnSocket(Socket socket) {
        if (socket instanceof SSLSocket) {
            String[] protocols = filterTLS((SSLSocket) socket);
            if (protocols.length > 0) {
                ((SSLSocket) socket).setEnabledProtocols(protocols);
            }
        }
        return socket;
    }

    private static final String TLS_v1 = "TLSv1";
    private static final String TLS_v1_1 = "TLSv1.1";
    private static final String TLS_v1_2 = "TLSv1.2";
    private static final String[] TLS_VERSIONS = {TLS_v1_2, TLS_v1_1, TLS_v1 };

    private static String[] filterTLS(SSLSocket sslSocket) {
        List<String> supported = Arrays.asList(sslSocket.getSupportedProtocols());
        Log.i("TLSSocketFactory", "is TLS enabled in server:" + supported);
        List<String> result = new ArrayList<>();
        for (String tlsVersion : TLS_VERSIONS) {
            if (supported.contains(tlsVersion)) {
                result.add(tlsVersion);
            }
        }
        Log.i("TLSSocketFactory", "Enabled protocols: "+result);
        return result.toArray(new String[0]);
    }
}