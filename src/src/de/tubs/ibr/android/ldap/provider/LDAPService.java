package de.tubs.ibr.android.ldap.provider;

import com.unboundid.ldap.sdk.AddRequest;
import com.unboundid.ldap.sdk.DeleteRequest;
import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.LDAPSearchException;
import com.unboundid.ldap.sdk.ModifyRequest;
import com.unboundid.ldap.sdk.SearchRequest;
import com.unboundid.ldap.sdk.SearchScope;
import de.tubs.ibr.android.ldap.auth.ServerInstance;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;

public class LDAPService extends Service {
  private Handler mCallbackHandler;
  private LDAPResultRunnable mRunnable;
  /**
   * The size limit that will be used for searches.
   */
  private static final int SIZE_LIMIT = 100;

  /**
   * The time limit (in seconds) that will be used for searches.
   */
  private static final int TIME_LIMIT_SECONDS = 30;

  /**
   * The LDAP Binder, which communicates with the given Handler
   */
  private final IBinder mBinder = new LDAPBinder();

  /*
   * (non-Javadoc)
   * @see android.app.Service#onBind(android.content.Intent)
   */
  @Override
  public IBinder onBind(Intent intent) {
    return mBinder;
  }

  public class LDAPBinder extends Binder {
    /**
     * @param callback
     */
    public void setActivityCallBackHandler(final Handler callback) {
      mCallbackHandler = callback;
    }

    /**
     * @param runnable
     */
    public void setRunnable(Runnable runnable) {
      mRunnable = (LDAPResultRunnable) runnable;
    }

    /**
     * Executes a {@link SearchRequest} an the LDAP Directory
     * 
     * @param instance
     * @param filter
     */
    public void search(final ServerInstance instance, final Filter filter) {
      search(instance, filter, SIZE_LIMIT, TIME_LIMIT_SECONDS);
    }

    /**
     * Executes a {@link SearchRequest} an the LDAP Directory
     * 
     * @param instance
     * @param filter
     * @param requestsizelimit
     */
    public void search(final ServerInstance instance, final Filter filter,
        final int requestsizelimit) {
      search(instance, filter, requestsizelimit, TIME_LIMIT_SECONDS);
    }

    /**
     * Executes a {@link SearchRequest} an the LDAP Directory
     * 
     * @param instance
     * @param filter
     * @param requestsizelimit
     * @param timelimitsec
     */
    public void search(final ServerInstance instance, final Filter filter,
        final int requestsizelimit, final int timelimitsec) {
      new Thread() {
        public void run() {
          LDAPConnection conn = null;
          try {
            conn = instance.getConnection();
            final SearchRequest request = new SearchRequest(
                instance.getBaseDN(), SearchScope.SUB, filter);
            request.setSizeLimit(requestsizelimit);
            request.setTimeLimitSeconds(timelimitsec);

            mRunnable.searchResult = conn.search(request);
          } catch (LDAPSearchException lse) {
            mRunnable.exception = lse;
            mRunnable.searchResult = lse.getSearchResult();
          } catch (LDAPException le) {
            mRunnable.exception = le;
            mRunnable.searchResult = new LDAPSearchException(le)
                .getSearchResult();
          } finally {
            if (conn != null) {
              conn.close();
            }
          }
          mCallbackHandler.post(mRunnable);
        }

      }.start();
    }

    /**
     * Executes the {@link AddRequest} on LDAP Directory
     * 
     * @param instance
     * @param request
     */
    public void insert(final ServerInstance instance, final AddRequest request) {
      new Thread() {
        public void run() {
          LDAPConnection conn = null;
          try {
            conn = instance.getConnection();
            conn.add(request);
          } catch (LDAPException e) {
            mRunnable.exception = e;
          } finally {
            if (conn != null) {
              conn.close();
            }
          }
          mCallbackHandler.post(mRunnable);
        }
      }.start();
    }

    /**
     * Executes the {@link DeleteRequest} on LDAP Directory
     * 
     * @param instance
     * @param deleteRequest
     */
    public void delete(final ServerInstance instance,
        final DeleteRequest deleteRequest) {
      new Thread() {
        public void run() {
          LDAPConnection conn = null;
          try {
            conn = instance.getConnection();
            mRunnable.deleteResult = conn.delete(deleteRequest);
          } catch (LDAPException e) {
            mRunnable.exception = e;
          } finally {
            if (conn != null) {
              conn.close();
            }
          }
          mCallbackHandler.post(mRunnable);
        }
      }.start();
    }

    /**
     * Executes the {@link ModifyRequest} on LDAP Directory
     * 
     * @param instance
     * @param deleteRequest
     */
    public void update(final ServerInstance instance,
        final ModifyRequest modifyRequest) {
      new Thread() {
        public void run() {
          LDAPConnection conn = null;
          try {
            conn = instance.getConnection();
            mRunnable.modificationResult = conn.modify(modifyRequest);
          } catch (LDAPException e) {
            mRunnable.exception = e;
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
