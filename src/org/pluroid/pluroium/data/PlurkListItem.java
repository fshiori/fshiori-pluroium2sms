/*
 * Copyright (C) 2011 The Pluroium Development Team.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.pluroid.pluroium.data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.graphics.Bitmap;

public class PlurkListItem {

    private Bitmap     avatar;
    private String    avatarIndex;
    private String     nickname;
    private String     qualifier;
    private String     qualifierTranslated;
    private String  rawContent;
    private String  content;
    private int     responses;
    private long    plurkId;
    private long    userId;
    private String    limitTo;
    private byte    hasSeen;
    private Date    posted;
    private int        favorites;
    private List<String>    favoriters;
    
    public PlurkListItem() {
        favoriters = new ArrayList<String>();
    }
    
    public PlurkListItem(PlurkListItem i) {
        avatar = i.avatar;
        avatarIndex = i.avatarIndex;
        nickname = i.nickname;
        qualifier = i.qualifier;
        qualifierTranslated = i.qualifierTranslated;
        content = i.content;
        responses = i.responses;
        plurkId = i.plurkId;
        userId = i.userId;
        limitTo = i.limitTo;
        hasSeen = i.hasSeen;
        posted = i.posted;
    }

    public Bitmap getAvatar() {
        return avatar;
    }

    public void setAvatar(Bitmap avatar) {
        this.avatar = avatar;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getQualifier() {
        return qualifier;
    }

    public void setQualifier(String qualifier) {
        this.qualifier = qualifier;
    }

    public String getQualifierTranslated() {
        return qualifierTranslated;
    }

    public void setQualifierTranslated(String qualifierTranslated) {
        this.qualifierTranslated = qualifierTranslated;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public int getResponses() {
        return responses;
    }

    public void setResponses(int responses) {
        this.responses = responses;
    }

    public long getPlurkId() {
        return plurkId;
    }

    public void setPlurkId(long plurkId) {
        this.plurkId = plurkId;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public String getLimitTo() {
        return limitTo;
    }

    public void setLimitTo(String limitTo) {
        this.limitTo = limitTo;
    }

    public byte isHasSeen() {
        return hasSeen;
    }

    public void setHasSeen(byte hasSeen) {
        this.hasSeen = hasSeen;
    }

    public Date getPosted() {
        return posted;
    }

    public void setPosted(Date posted) {
        this.posted = posted;
    }

    public byte getHasSeen() {
        return hasSeen;
    }

    public String getAvatarIndex() {
        return avatarIndex;
    }

    public void setAvatarIndex(String avatarIndex) {
        this.avatarIndex = avatarIndex;
    }

    public String getRawContent() {
        return rawContent;
    }

    public void setRawContent(String rawContent) {
        this.rawContent = rawContent;
    }

    public int getFavorites() {
        return favorites;
    }

    public void setFavorites(int favorites) {
        this.favorites = favorites;
    }

    public List<String> getFavoriters() {
        return favoriters;
    }

    public void setFavoriters(List<String> favoriters) {
        this.favoriters = favoriters;
    }

}
