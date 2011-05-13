package de.tubs.ibr.android.ldap.provider;

import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.LDAPSearchException;
import com.unboundid.ldap.sdk.SearchRequest;
import com.unboundid.ldap.sdk.SearchScope;
import de.tubs.ibr.android.ldap.auth.ServerInstance;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;

public class SearchService extends Service {
  private Handler mCallbackHandler;
  private SearchResultRunnable mRunnable;
  /**
   * The size limit that will be used for searches.
   */
  static final int SIZE_LIMIT = 100;

  /**
   * The time limit (in seconds) that will be used for searches.
   */
  static final int TIME_LIMIT_SECONDS = 30;

  private final IBinder mBinder = new SearchBinder();

  @Override
  public IBinder onBind(Intent arg0) {
    return mBinder;
  }

  public class SearchBinder extends Binder {
    public void setActivityCallBackHandler(final Handler callback) {
      mCallbackHandler = callback;
    }

    public void setRunnable(final Runnable runnable) {
      mRunnable = (SearchResultRunnable) runnable;
    }

    public void search(final ServerInstance instance, final Filter filter) {
      new Thread() {
        public void run() {
          LDAPConnection conn = null;
          try {
            conn = instance.getConnection();
            final SearchRequest request = new SearchRequest(
                instance.getBaseDN(), SearchScope.SUB, filter);
            request.setSizeLimit(SIZE_LIMIT);
            request.setTimeLimitSeconds(TIME_LIMIT_SECONDS);

            mRunnable.result = conn.search(request);
          } catch (LDAPSearchException lse) {

            mRunnable.result = lse.getSearchResult();
          } catch (LDAPException le) {

            mRunnable.result = new LDAPSearchException(le).getSearchResult();
          } finally {
            if (conn != null) {
              conn.close();
            }
          }
          mCallbackHandler.post(mRunnable);
        }

      }.start();
    }
  }

}
