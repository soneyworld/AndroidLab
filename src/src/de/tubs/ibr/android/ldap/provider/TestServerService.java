package de.tubs.ibr.android.ldap.provider;

import static com.unboundid.util.StaticUtils.getExceptionMessage;
import java.util.LinkedList;
import com.unboundid.ldap.sdk.Entry;
import com.unboundid.ldap.sdk.LDAPConnection;
import de.tubs.ibr.android.ldap.R;
import de.tubs.ibr.android.ldap.auth.ServerInstance;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;

public class TestServerService extends Service {
  private Handler mCallbackHandler;
  private TestServerRunnable mRunnable;
  private final IBinder mBinder = new TestServerBinder();

  @Override
  public IBinder onBind(Intent intent) {
    return mBinder;
  }

  public class TestServerBinder extends Binder {
    public void setActivityCallBackHandler(final Handler callback) {
      mCallbackHandler = callback;
    }

    public void setRunnable(Runnable runnable) {
      mRunnable = (TestServerRunnable) runnable;
    }

    public void test(final ServerInstance instance) {
      new Thread() {
        public void run() {
          final LinkedList<String> reasons = new LinkedList<String>();
          boolean acceptable = instance.isDefinitionValid(reasons, mRunnable.context);
          if (acceptable) {
            LDAPConnection conn = null;
            try {
              conn = instance.getConnection();

              final Entry e = conn.getEntry(instance.getBaseDN());
              if (e == null) {
                acceptable = false;
                reasons.add(mRunnable.context.getString(
                    R.string.test_server_thread_cannot_get_base,
                    instance.getBaseDN()));
              }
            } catch (Exception e) {

              acceptable = false;
              reasons.add(getExceptionMessage(e));
            } finally {
              if (conn != null) {
                conn.close();
              }
            }
          }
          mRunnable.reasons = reasons;
          mRunnable.acceptable = acceptable;
          mCallbackHandler.post(mRunnable);
        }
      }.start();
    }
  }
}
