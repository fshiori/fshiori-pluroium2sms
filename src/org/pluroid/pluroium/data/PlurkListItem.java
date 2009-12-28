package org.pluroid.pluroium.data;

import android.graphics.Bitmap;
import android.text.SpannableStringBuilder;

public class PlurkListItem {

	private Bitmap 	avatar;
	private String	avatarIndex;
	private String 	nickname;
	private String 	qualifier;
	private String 	qualifierTranslated;
	private String  rawContent;
	private SpannableStringBuilder content;
	private int 	responses;
	private long	plurkId;
	private long	userId;
	private String	limitTo;
	private byte	hasSeen;
	private String  utcPosted;
	private String	posted;
	
	public PlurkListItem() {
		
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
		utcPosted = i.utcPosted;
		posted = i.posted;
	}

	/*
	private PlurkListItem(Parcel in) {
		avatar = in.readParcelable(null);
		avatarIndex = in.readString();
		nickname = in.readString();
		qualifier = in.readString();
		qualifierTranslated = in.readString();
		content = in.readString();
		responses = in.readInt();
		plurkId = in.readLong();
		userId = in.readLong();
		limitTo = in.readString();
		hasSeen = in.readByte();
		posted = in.readString();
	}
	
	public static final Parcelable.Creator<PlurkListItem> CREATOR =
		new Parcelable.Creator<PlurkListItem>() {
			
			public PlurkListItem createFromParcel(Parcel p) {
				PlurkListItem item = new PlurkListItem(p);
				
				if (item == null) {
					throw new RuntimeException("Failed to unparcel PlurkListItem");
				}
				
				return item;
			}
			
			public PlurkListItem[] newArray(int size) {
				return new PlurkListItem[size];
			}
		
		};
	
	
	public int describeContents() {
		return 0;
	}

	public void writeToParcel(Parcel dest, int flags) {
		dest.writeParcelable(avatar, flags);
		dest.writeString(avatarIndex);
		dest.writeString(nickname);
		dest.writeString(qualifier);
		dest.writeString(qualifierTranslated);
		dest.writeString(content);
		dest.writeInt(responses);
		dest.writeLong(plurkId);
		dest.writeLong(userId);
		dest.writeString(limitTo);
		dest.writeByte(hasSeen);
		dest.writeString(posted);
	}
	*/

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

	public SpannableStringBuilder getContent() {
		return content;
	}

	public void setContent(SpannableStringBuilder content) {
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

	public String getPosted() {
		return posted;
	}

	public void setPosted(String posted) {
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

	public String getUtcPosted() {
		return utcPosted;
	}

	public void setUtcPosted(String utcPosted) {
		this.utcPosted = utcPosted;
	}

	public String getRawContent() {
		return rawContent;
	}

	public void setRawContent(String rawContent) {
		this.rawContent = rawContent;
	}

}
