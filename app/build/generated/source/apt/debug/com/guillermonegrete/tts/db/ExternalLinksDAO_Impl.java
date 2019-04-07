package com.guillermonegrete.tts.db;

import android.database.Cursor;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings({"unchecked", "deprecation"})
public final class ExternalLinksDAO_Impl implements ExternalLinksDAO {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter __insertionAdapterOfExternalLink;

  private final SharedSQLiteStatement __preparedStmtOfDeleteAll;

  public ExternalLinksDAO_Impl(RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfExternalLink = new EntityInsertionAdapter<ExternalLink>(__db) {
      @Override
      public String createQuery() {
        return "INSERT OR IGNORE INTO `links`(`lid`,`site`,`link`,`language`) VALUES (nullif(?, 0),?,?,?)";
      }

      @Override
      public void bind(SupportSQLiteStatement stmt, ExternalLink value) {
        stmt.bindLong(1, value.id);
        if (value.siteName == null) {
          stmt.bindNull(2);
        } else {
          stmt.bindString(2, value.siteName);
        }
        if (value.link == null) {
          stmt.bindNull(3);
        } else {
          stmt.bindString(3, value.link);
        }
        if (value.language == null) {
          stmt.bindNull(4);
        } else {
          stmt.bindString(4, value.language);
        }
      }
    };
    this.__preparedStmtOfDeleteAll = new SharedSQLiteStatement(__db) {
      @Override
      public String createQuery() {
        final String _query = "DELETE FROM links";
        return _query;
      }
    };
  }

  @Override
  public long insert(final ExternalLink link) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      long _result = __insertionAdapterOfExternalLink.insertAndReturnId(link);
      __db.setTransactionSuccessful();
      return _result;
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void insert(final ExternalLink... links) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __insertionAdapterOfExternalLink.insert(links);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void deleteAll() {
    __db.assertNotSuspendingTransaction();
    final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteAll.acquire();
    __db.beginTransaction();
    try {
      _stmt.executeUpdateDelete();
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
      __preparedStmtOfDeleteAll.release(_stmt);
    }
  }

  @Override
  public List<ExternalLink> findLinksByLanguage(final String language) {
    final String _sql = "SELECT * FROM links where language = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    if (language == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, language);
    }
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false);
    try {
      final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "lid");
      final int _cursorIndexOfSiteName = CursorUtil.getColumnIndexOrThrow(_cursor, "site");
      final int _cursorIndexOfLink = CursorUtil.getColumnIndexOrThrow(_cursor, "link");
      final int _cursorIndexOfLanguage = CursorUtil.getColumnIndexOrThrow(_cursor, "language");
      final List<ExternalLink> _result = new ArrayList<ExternalLink>(_cursor.getCount());
      while(_cursor.moveToNext()) {
        final ExternalLink _item;
        final String _tmpSiteName;
        _tmpSiteName = _cursor.getString(_cursorIndexOfSiteName);
        final String _tmpLink;
        _tmpLink = _cursor.getString(_cursorIndexOfLink);
        final String _tmpLanguage;
        _tmpLanguage = _cursor.getString(_cursorIndexOfLanguage);
        _item = new ExternalLink(_tmpSiteName,_tmpLink,_tmpLanguage);
        _item.id = _cursor.getInt(_cursorIndexOfId);
        _result.add(_item);
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }
}
