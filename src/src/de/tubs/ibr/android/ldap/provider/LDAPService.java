package de.tubs.ibr.android.ldap.provider;

import java.util.LinkedList;
import com.unboundid.ldap.sdk.AddRequest;
import com.unboundid.ldap.sdk.DeleteRequest;
import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.LDAPSearchException;
import com.unboundid.ldap.sdk.ModifyRequest;
import com.unboundid.ldap.sdk.SearchRequest;
import com.unboundid.ldap.sdk.SearchResult;
import com.unboundid.ldap.sdk.SearchResultEntry;
import com.unboundid.ldap.sdk.SearchScope;
import de.tubs.ibr.android.ldap.auth.ServerInstance;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
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
                instance.getBaseDN(), SearchScope.SUB, filter,
                SearchRequest.ALL_OPERATIONAL_ATTRIBUTES,
                SearchRequest.ALL_USER_ATTRIBUTES);
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

    /**
     * Searches for organizational units to find directories where entries can
     * be saved
     * 
     * @param instance
     */
    public void searchDirs(final ServerInstance instance) {
      if (instance != null)
        new Thread() {
          public void run() {
            LDAPConnection conn = null;
            try {
              conn = instance.getConnection();
              final Filter filter = Filter.create("(objectClass=top)");
              final SearchRequest request = new SearchRequest(
                  instance.getBaseDN(), SearchScope.SUB, filter,
                  SearchRequest.ALL_OPERATIONAL_ATTRIBUTES);
              request.setSizeLimit(SIZE_LIMIT);
              request.setTimeLimitSeconds(TIME_LIMIT_SECONDS);
              SearchResult result = conn.search(request);
              LinkedList<String> dirs = new LinkedList<String>();
              for (SearchResultEntry entry : result.getSearchEntries()) {
                if (!entry.getAttributeValue("hasSubordinates").equals("FALSE"))
                  dirs.add(entry.getDN());
              }
              mRunnable.dirsResult = new String[dirs.size()];
              SharedPreferences dirPrefs = getSharedPreferences("dirs_"
                  + instance.getID(), MODE_PRIVATE);
              Editor editor = dirPrefs.edit();
              int i = 0;
              for (String dir : dirs) {
                if (!dirPrefs.getAll().containsKey(dir))
                  editor.putString("" + dir, "");
                mRunnable.dirsResult[i] = dir;
                i++;
              }
              for (String key : dirPrefs.getAll().keySet()) {
                if (!dirs.contains(key)) {
                  editor.remove(key);
                }
              }
              editor.commit();
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
  }
}
