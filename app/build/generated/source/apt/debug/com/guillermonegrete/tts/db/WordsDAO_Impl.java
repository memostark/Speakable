package com.guillermonegrete.tts.db;

import android.database.Cursor;
import androidx.lifecycle.LiveData;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Exception;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

@SuppressWarnings({"unchecked", "deprecation"})
public final class WordsDAO_Impl implements WordsDAO {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter __insertionAdapterOfWords;

  private final EntityDeletionOrUpdateAdapter __updateAdapterOfWords;

  private final SharedSQLiteStatement __preparedStmtOfDeleteWordById;

  private final SharedSQLiteStatement __preparedStmtOfDeleteWord;

  private final SharedSQLiteStatement __preparedStmtOfDeleteAll;

  public WordsDAO_Impl(RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfWords = new EntityInsertionAdapter<Words>(__db) {
      @Override
      public String createQuery() {
        return "INSERT OR IGNORE INTO `words`(`wid`,`word`,`lang`,`definition`,`notes`) VALUES (nullif(?, 0),?,?,?,?)";
      }

      @Override
      public void bind(SupportSQLiteStatement stmt, Words value) {
        stmt.bindLong(1, value.id);
        if (value.word == null) {
          stmt.bindNull(2);
        } else {
          stmt.bindString(2, value.word);
        }
        if (value.lang == null) {
          stmt.bindNull(3);
        } else {
          stmt.bindString(3, value.lang);
        }
        if (value.definition == null) {
          stmt.bindNull(4);
        } else {
          stmt.bindString(4, value.definition);
        }
        if (value.notes == null) {
          stmt.bindNull(5);
        } else {
          stmt.bindString(5, value.notes);
        }
      }
    };
    this.__updateAdapterOfWords = new EntityDeletionOrUpdateAdapter<Words>(__db) {
      @Override
      public String createQuery() {
        return "UPDATE OR IGNORE `words` SET `wid` = ?,`word` = ?,`lang` = ?,`definition` = ?,`notes` = ? WHERE `wid` = ?";
      }

      @Override
      public void bind(SupportSQLiteStatement stmt, Words value) {
        stmt.bindLong(1, value.id);
        if (value.word == null) {
          stmt.bindNull(2);
        } else {
          stmt.bindString(2, value.word);
        }
        if (value.lang == null) {
          stmt.bindNull(3);
        } else {
          stmt.bindString(3, value.lang);
        }
        if (value.definition == null) {
          stmt.bindNull(4);
        } else {
          stmt.bindString(4, value.definition);
        }
        if (value.notes == null) {
          stmt.bindNull(5);
        } else {
          stmt.bindString(5, value.notes);
        }
        stmt.bindLong(6, value.id);
      }
    };
    this.__preparedStmtOfDeleteWordById = new SharedSQLiteStatement(__db) {
      @Override
      public String createQuery() {
        final String _query = "DELETE FROM words where wid = ?";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteWord = new SharedSQLiteStatement(__db) {
      @Override
      public String createQuery() {
        final String _query = "DELETE FROM words where word = ?";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteAll = new SharedSQLiteStatement(__db) {
      @Override
      public String createQuery() {
        final String _query = "DELETE FROM words";
        return _query;
      }
    };
  }

  @Override
  public long insert(final Words word) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      long _result = __insertionAdapterOfWords.insertAndReturnId(word);
      __db.setTransactionSuccessful();
      return _result;
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void insert(final Words... words) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __insertionAdapterOfWords.insert(words);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void update(final Words words) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __updateAdapterOfWords.handle(words);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void deleteWordById(final int id) {
    __db.assertNotSuspendingTransaction();
    final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteWordById.acquire();
    int _argIndex = 1;
    _stmt.bindLong(_argIndex, id);
    __db.beginTransaction();
    try {
      _stmt.executeUpdateDelete();
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
      __preparedStmtOfDeleteWordById.release(_stmt);
    }
  }

  @Override
  public void deleteWord(final String word) {
    __db.assertNotSuspendingTransaction();
    final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteWord.acquire();
    int _argIndex = 1;
    if (word == null) {
      _stmt.bindNull(_argIndex);
    } else {
      _stmt.bindString(_argIndex, word);
    }
    __db.beginTransaction();
    try {
      _stmt.executeUpdateDelete();
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
      __preparedStmtOfDeleteWord.release(_stmt);
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
  public Words findWordById(final int id) {
    final String _sql = "SELECT * FROM words where wid = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, id);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false);
    try {
      final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "wid");
      final int _cursorIndexOfWord = CursorUtil.getColumnIndexOrThrow(_cursor, "word");
      final int _cursorIndexOfLang = CursorUtil.getColumnIndexOrThrow(_cursor, "lang");
      final int _cursorIndexOfDefinition = CursorUtil.getColumnIndexOrThrow(_cursor, "definition");
      final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
      final Words _result;
      if(_cursor.moveToFirst()) {
        final String _tmpWord;
        _tmpWord = _cursor.getString(_cursorIndexOfWord);
        final String _tmpLang;
        _tmpLang = _cursor.getString(_cursorIndexOfLang);
        final String _tmpDefinition;
        _tmpDefinition = _cursor.getString(_cursorIndexOfDefinition);
        _result = new Words(_tmpWord,_tmpLang,_tmpDefinition);
        _result.id = _cursor.getInt(_cursorIndexOfId);
        _result.notes = _cursor.getString(_cursorIndexOfNotes);
      } else {
        _result = null;
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @Override
  public Words findWord(final String word) {
    final String _sql = "SELECT * FROM words where word = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    if (word == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, word);
    }
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false);
    try {
      final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "wid");
      final int _cursorIndexOfWord = CursorUtil.getColumnIndexOrThrow(_cursor, "word");
      final int _cursorIndexOfLang = CursorUtil.getColumnIndexOrThrow(_cursor, "lang");
      final int _cursorIndexOfDefinition = CursorUtil.getColumnIndexOrThrow(_cursor, "definition");
      final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
      final Words _result;
      if(_cursor.moveToFirst()) {
        final String _tmpWord;
        _tmpWord = _cursor.getString(_cursorIndexOfWord);
        final String _tmpLang;
        _tmpLang = _cursor.getString(_cursorIndexOfLang);
        final String _tmpDefinition;
        _tmpDefinition = _cursor.getString(_cursorIndexOfDefinition);
        _result = new Words(_tmpWord,_tmpLang,_tmpDefinition);
        _result.id = _cursor.getInt(_cursorIndexOfId);
        _result.notes = _cursor.getString(_cursorIndexOfNotes);
      } else {
        _result = null;
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  @Override
  public LiveData<List<Words>> getAllWords() {
    final String _sql = "SELECT * FROM words";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return __db.getInvalidationTracker().createLiveData(new String[]{"words"}, false, new Callable<List<Words>>() {
      @Override
      public List<Words> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "wid");
          final int _cursorIndexOfWord = CursorUtil.getColumnIndexOrThrow(_cursor, "word");
          final int _cursorIndexOfLang = CursorUtil.getColumnIndexOrThrow(_cursor, "lang");
          final int _cursorIndexOfDefinition = CursorUtil.getColumnIndexOrThrow(_cursor, "definition");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final List<Words> _result = new ArrayList<Words>(_cursor.getCount());
          while(_cursor.moveToNext()) {
            final Words _item;
            final String _tmpWord;
            _tmpWord = _cursor.getString(_cursorIndexOfWord);
            final String _tmpLang;
            _tmpLang = _cursor.getString(_cursorIndexOfLang);
            final String _tmpDefinition;
            _tmpDefinition = _cursor.getString(_cursorIndexOfDefinition);
            _item = new Words(_tmpWord,_tmpLang,_tmpDefinition);
            _item.id = _cursor.getInt(_cursorIndexOfId);
            _item.notes = _cursor.getString(_cursorIndexOfNotes);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }
}
