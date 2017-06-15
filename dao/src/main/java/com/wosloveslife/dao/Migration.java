package com.wosloveslife.dao;

import io.realm.DynamicRealm;
import io.realm.FieldAttribute;
import io.realm.RealmMigration;
import io.realm.RealmSchema;

public class Migration implements RealmMigration {
    public static final long SCHEMA_VERSION = 1;

    @Override
    public void migrate(DynamicRealm realm, long oldVersion, long newVersion) {
        RealmSchema schema = realm.getSchema();

        if (oldVersion == 0) {
            // 声音文件
            schema.create("Audio")
                    .addField(Audio.ID, long.class, FieldAttribute.PRIMARY_KEY)
                    .addField(Audio.TITLE, String.class)
                    .addField(Audio.ARTIST, String.class)
                    .addField(Audio.ALBUM, String.class)
                    .addField(Audio.TITLE_PINYIN, String.class)
                    .addField(Audio.ARTIST_PINYIN, String.class)
                    .addField(Audio.ALBUM_PINYIN, String.class)
                    .addField(Audio.PATH, String.class)
                    .addField(Audio.DURATION, long.class)
                    .addField(Audio.SIZE, long.class)
                    .addField(Audio.YEAR, int.class)
                    .addField(Audio.TRACK, int.class)
                    .addField(Audio.DISC_ID, int.class)
                    .addField(Audio.IS_MUSIC, boolean.class)
                    .addField(Audio.IS_ALARM, boolean.class)
                    .addField(Audio.IS_RINGTONE, boolean.class)
                    .addField(Audio.IS_PODCAST, boolean.class)
                    .addField(Audio.IS_NOTIFICATION, boolean.class)
                    .addField(Audio.JOIN_TIMESTAMP, long.class);

            // 文件夹
            schema.create("SongList")
                    .addField(SongList.ID, long.class, FieldAttribute.PRIMARY_KEY)
                    .addField(SongList.TITLE, String.class)
                    .addField(SongList.AUTHOR, String.class)
                    .addField(SongList.TITLE_PINYIN, String.class)
                    .addField(SongList.AUTHOR_PINYIN, String.class)
                    .addRealmListField(SongList.SONGS, schema.get("RealmUser"))
                    .addField(SongList.CREATE_TIMESTAMP, long.class)
                    .addField(SongList.MODIFY_TIMESTAMP, long.class);

            oldVersion++;
        }

        if (oldVersion != SCHEMA_VERSION) {
            throw new RuntimeException("unexpected scheme version. expected: " + SCHEMA_VERSION + ", actual: " + oldVersion);
        }
    }
}