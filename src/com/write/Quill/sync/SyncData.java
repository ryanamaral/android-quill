package com.write.Quill.sync;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.UUID;

import junit.framework.Assert;

import com.write.Quill.data.Bookshelf;
import com.write.Quill.data.Bookshelf.BookPreview;

import android.content.SharedPreferences;
import android.text.format.Time;
import android.util.Log;

/**
 * The sync data for all books
 * @author vbraun
 */
public class SyncData implements java.lang.Iterable<SyncData.SyncItem> {
	private final static String TAG = "SyncData";
	
	protected enum Command {
		METADATA_ONLY, SYNC_ONLY, FULL_SYNC
	}

	protected Command command = Command.METADATA_ONLY;
	
	protected enum State { 
		IN_SYNC, LOCAL_ONLY, LOCAL_IS_NEWER, REMOTE_IS_NEWER, CONFLICT 
	}

	protected enum Action {
		SKIP, PUSH_TO_SERVER, PULL_TO_ANDROID
	}
	
	public class SyncItem {
		private State state;
		private Action action;
		
		private boolean local = false;
		private boolean remote = false;
		private String localTitle, remoteTitle;
		private final UUID uuid;
		
		// mtime of the book at last sync
		
		protected final Time lastSync;
		// the last-modified time
		protected Time localTime, remoteTime;
	
		protected SyncItem(UUID uuid) {
			this.uuid = uuid;
			long millis = syncPrefs.getLong(uuid.toString(), 0);
			if (millis > 0) {
				lastSync = new Time();
				lastSync.set(millis);
			} else 
				lastSync = null;
		}
		
		protected void setLocal(BookPreview book) {
			local = true;
			Assert.assertTrue(this.uuid.equals(uuid));
			localTitle = book.getTitle();
			localTime = book.getLastModifiedTime();
			state = null;
		}
		
		protected void setRemote(UUID uuid, String title, Time mtime) {
			if (remote && mtime.before(remoteTime)) {
				// Log.e(TAG, "skipping remote "+uuid + " " + mtime.toMillis(false));
				return;   // is an older backup
			}
			// Log.e(TAG, "adding remote "+uuid + " " + mtime.toMillis(false));
			remote = true;
			Assert.assertTrue(this.uuid.equals(uuid));
			remoteTitle = title;
			remoteTime = mtime;
			state = null;
		}
		
		private State computeState() {
			Assert.assertTrue(local || remote);
			if (!local)
				return State.REMOTE_IS_NEWER;
			if (!remote) 
				return State.LOCAL_ONLY;
			//Log.e(TAG, "time uuid = "+uuid);
			//Log.e(TAG, "time local  "+localTime.toMillis(false));
			//Log.e(TAG, "time remote "+remoteTime.toMillis(false));
			if (Time.compare(localTime, remoteTime) == 0)
				return State.IN_SYNC;
			if (lastSync == null)
				return State.CONFLICT;
			//Log.e(TAG, "time last   "+lastSync.toMillis(false));
			boolean localIsUnchanged  = (Time.compare(localTime,  lastSync) == 0);
			boolean remoteIsUnchanged = (Time.compare(remoteTime, lastSync) == 0);
			if (localTime.before(remoteTime) && localIsUnchanged)
				return State.REMOTE_IS_NEWER;
			if (localTime.after(remoteTime) && remoteIsUnchanged)
				return State.LOCAL_IS_NEWER;
			return State.CONFLICT;
		}
		
		protected Action computeDefaultAction() {
			switch (getState()) {
			case LOCAL_IS_NEWER:
				return Action.PUSH_TO_SERVER;
			case REMOTE_IS_NEWER: 
				return Action.PULL_TO_ANDROID;
			default:
				return Action.SKIP;
			}
		}
				
		protected UUID getUuid() {
			return uuid;
		}
		
		protected boolean isOnLocal() {
			return local;
		}
		
		protected boolean isOnRemote() {
			return remote;
		}
		
		protected State getState() {
			if (state == null)
				state = computeState();
			return state;
		}
		
		protected Action getAction() {
			if (action == null)
				action = computeDefaultAction();
			return action;
		}
		
		protected void cycleAction() {
			switch (getAction()) {
			case PUSH_TO_SERVER:
				if (remote) 
					action = Action.PULL_TO_ANDROID; 
				else
					action = Action.SKIP;
				break;
			case PULL_TO_ANDROID:
				action = Action.SKIP;
				break;
			case SKIP:
				if (local)
					action = Action.PUSH_TO_SERVER;
				else if (remote)
					action = Action.PULL_TO_ANDROID;
				break;
			}
		}
		
		protected void setAction(Action action) {
			this.action = action;
		}
		
		protected Time getLastModTime() {
			Assert.assertTrue(local || remote);
			if (!local)
				return remoteTime;
			if (!remote) 
				return localTime;
			if (localTime.after(remoteTime))
				return localTime;
			else
				return remoteTime;
		}
		
		protected Time getRemoteModTime() {
			return remoteTime;
		}
		
		protected Time getLocalModTime() {
			return localTime;
		}

		protected String getTitle() {
			switch (getState()) {
			case CONFLICT: 
			case LOCAL_IS_NEWER:
			case LOCAL_ONLY:
			case IN_SYNC:
				return localTitle;
			case REMOTE_IS_NEWER:
				return remoteTitle;
			}
			Assert.fail(); // unreachable
			return null;
		}
		
		public String toString() {
			return getTitle();
		}
		
		protected void saveSyncTime(Time time) {
			SharedPreferences.Editor editor = syncPrefs.edit();
			editor.putLong(getUuid().toString(), time.toMillis(false));
			// Log.e(TAG, "saveSyncTime "+getUuid().toString() + " " + time.toMillis(false));
			editor.commit();
		}
	}
	
	// The actual sync data
	protected final LinkedList<SyncItem> data;
	private final SharedPreferences syncPrefs;

	// keep authentication data here for convenience
	protected final QuillAccount account;
	protected String sessionToken;
	
	public SyncData(SharedPreferences syncPreferences, QuillAccount account) {
		data = new LinkedList<SyncItem>();
		syncPrefs = syncPreferences;
		this.account = account;
		initLocal();
	}
	
	public SyncData copy() {
		SyncData s = new SyncData(syncPrefs, account);
		s.sessionToken = sessionToken;
		s.command = command;
		return s;
	}
	
	public static class SyncItemComparator implements Comparator<SyncItem> {
		@Override
		public int compare(SyncItem lhs, SyncItem rhs) {
			SyncData.State lhsState = lhs.getState();
			SyncData.State rhsState = rhs.getState();
			// Conflict is on top
			if ((!lhsState.equals(State.CONFLICT)) && ( rhsState.equals(State.CONFLICT))) return +1;
			if (( lhsState.equals(State.CONFLICT)) && (!rhsState.equals(State.CONFLICT))) return -1;

			// Local-only is next to bottom
			if (( lhsState.equals(State.LOCAL_ONLY)) && (!rhsState.equals(State.LOCAL_ONLY))) return +1;
			if ((!lhsState.equals(State.LOCAL_ONLY)) && ( rhsState.equals(State.LOCAL_ONLY))) return -1;

			// in sync is at bottom
			if (( lhsState.equals(State.IN_SYNC)) && (!rhsState.equals(State.IN_SYNC))) return +1;
			if ((!lhsState.equals(State.IN_SYNC)) && ( rhsState.equals(State.IN_SYNC))) return -1;

			// otherwise sort by time			
			Time lhsTime = lhs.getLastModTime();
			Time rhsTime = rhs.getLastModTime();
			if (lhsTime.before(rhsTime)) return +1;
			if (lhsTime.after(rhsTime)) return -1;
			
			// if all else fails, sort by uuid
			return lhs.getUuid().toString().compareToIgnoreCase(rhs.getUuid().toString());
		}
	}
	
	protected void addLocal(BookPreview book) {
		UUID uuid = book.getUUID();
		// Log.e(TAG, "addLocal "+uuid);
		for (SyncItem item : data)
			if (item.getUuid().equals(uuid)) {
				item.setLocal(book);
				return;
			}
		SyncItem item = new SyncItem(uuid);
		item.setLocal(book);
		data.add(item);
	}
	
	protected void addRemote(UUID uuid, String title, Time mtime) {
		for (SyncItem item : data)
			if (item.getUuid().equals(uuid)) {
				item.setRemote(uuid, title, mtime);
				return;
			}
		SyncItem item = new SyncItem(uuid);
		item.setRemote(uuid, title, mtime);
		data.add(item);
	}
	
	public int size() {
		return data.size();
	}
	
	protected void sort() {
		Collections.sort(data, new SyncItemComparator());
	}
		
	protected void initLocal() {
		data.clear();		
		for (BookPreview book : Bookshelf.getBookPreviewList())
			addLocal(book);
		sort();
	}
	
	protected SyncItem get(int i) {
		return data.get(i);
	}
	
	@Override
	public Iterator<SyncItem> iterator() {
		return data.iterator();
	}
	
	protected QuillAccount getAccount() {
		return account;
	}
	
	protected void setSessionToken(String token) {
		sessionToken = token;
	}
	
	protected String getSessionToken() {
		return sessionToken;
	}
		
}
