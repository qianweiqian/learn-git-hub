package com.mediatek.dialer.dialersearch;

import java.util.ArrayList;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.graphics.Typeface;
import android.net.Uri;
import android.provider.CallLog.Calls;
import android.provider.ContactsContract.CommonDataKinds;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.DialerSearch;
import android.telephony.PhoneNumberUtils;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.text.format.DateFormat;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.QuickContactBadge;
import android.widget.TextView;

import com.android.contacts.common.ContactPhotoManager;
import com.android.contacts.common.ContactPhotoManager.DefaultImageRequest;
import com.android.contacts.common.GeoUtil;
import com.android.contacts.common.format.TextHighlighter;
import com.android.contacts.common.list.ContactListItemView;

import com.android.contacts.common.list.PhoneNumberListAdapter;
import com.android.dialer.R;
import com.android.dialer.calllog.CallTypeIconsView;
import com.android.dialer.calllog.PhoneAccountUtils;
import com.android.dialer.calllog.ContactInfoHelper;
import com.android.dialer.calllog.PhoneNumberDisplayHelper;
import com.android.dialer.calllog.PhoneNumberUtilsWrapper;

//import com.mediatek.contacts.simcontact.SlotUtils;
//import com.mediatek.dialer.calllog.CallLogSimInfoHelper;
import com.mediatek.common.MPlugin;
import com.mediatek.common.telephony.ICallerInfoExt;
import com.mediatek.contacts.util.SimContactPhotoUtils;
import com.mediatek.dialer.ext.ExtensionManager;
import com.mediatek.dialer.util.DialerFeatureOptions;
import com.mediatek.dialer.util.DialerSearchUtils;
//import com.mediatek.dialer.util.DialerUtils;
import com.mediatek.dialer.util.LogUtils;
//import com.mediatek.phone.SIMInfoWrapper;
//qianweiqiang add boway for location hide 2015 7 15
import com.android.dialer.DialtactsActivity;
import android.content.SharedPreferences;
//qianweiqiang add boway for location hide 2015 7 15
/**
 * {@link PhoneNumberListAdapter} with the following added shortcuts, that are displayed as list
 * items:
 * 1) Directly calling the phone number query
 * 2) Adding the phone number query to a contact
 *
 * These shortcuts can be enabled or disabled to toggle whether or not they show up in the
 * list.
 */
public class DialerPhoneNumberListAdapterEx extends PhoneNumberListAdapter {

    private final String TAG = "DialerPhoneNumberListAdapterEx";

    private String mFormattedQueryString;
    private String mCountryIso;

    private final int VIEW_TYPE_UNKNOWN = -1;
    private final int VIEW_TYPE_CONTACT = 0;
    private final int VIEW_TYPE_CALL_LOG = 1;

    private final int NUMBER_TYPE_NORMAL = 0;
    private final int NUMBER_TYPE_UNKNOWN = 1;
    private final int NUMBER_TYPE_VOICEMAIL = 2;
    private final int NUMBER_TYPE_PRIVATE = 3;
    private final int NUMBER_TYPE_PAYPHONE = 4;
    private final int NUMBER_TYPE_EMERGENCY = 5;

    private final int DS_MATCHED_DATA_INIT_POS    = 3;
    private final int DS_MATCHED_DATA_DIVIDER     = 3;

    public final int NAME_LOOKUP_ID_INDEX        = 0;
    public final int CONTACT_ID_INDEX            = 1;
    public final int DATA_ID_INDEX               = 2;
    public final int CALL_LOG_DATE_INDEX         = 3;
    public final int CALL_LOG_ID_INDEX           = 4;
    public final int CALL_TYPE_INDEX             = 5;
    public final int CALL_GEOCODED_LOCATION_INDEX = 6;
    public final int PHONE_ACCOUNT_ID_INDEX                = 7;
    public final int PHONE_ACCOUNT_COMPONENT_NAME_INDEX     = 8;
    public final int PRESENTATION_INDEX          = 9;
    public final int INDICATE_PHONE_SIM_INDEX    = 10;
    public final int CONTACT_STARRED_INDEX       = 11;
    public final int PHOTO_ID_INDEX              = 12;
    public final int SEARCH_PHONE_TYPE_INDEX     = 13;
    public final int SEARCH_PHONE_LABEL_INDEX    = 14;
    public final int NAME_INDEX                  = 15;
    public final int SEARCH_PHONE_NUMBER_INDEX   = 16;
    public final int CONTACT_NAME_LOOKUP_INDEX   = 17;
    public final int IS_SDN_CONTACT              = 18;
    public final int DS_MATCHED_DATA_OFFSETS     = 19;
    public final int DS_MATCHED_NAME_OFFSETS     = 20;

    private ContactPhotoManager mContactPhotoManager;
    private final PhoneNumberUtilsWrapper mPhoneNumberUtils;
    private PhoneNumberDisplayHelper mPhoneNumberHelper;

    private String mUnknownNumber;
    private String mPrivateNumber;
    private String mPayphoneNumber;

    private String mEmergency;

    private String mVoiceMail;

    private Drawable[] mCallTypeDrawables = new Drawable[6];

    private TextHighlighter mTextHighlighter;

    public final static int SHORTCUT_INVALID = -1;
    ///M: [VoLTE] For Volte Call
    public final static int SHORTCUT_MAKE_VOLTE_CALL = 0;
    public final static int SHORTCUT_DIRECT_CALL = 1;
    public final static int SHORTCUT_ADD_NUMBER_TO_CONTACTS = 2;
    public final static int SHORTCUT_MAKE_VIDEO_CALL = 3;

    public final static int SHORTCUT_COUNT = 4;

    private final boolean[] mShortcutEnabled = new boolean[SHORTCUT_COUNT];

    public DialerPhoneNumberListAdapterEx(Context context) {
        super(context);

        mCountryIso = GeoUtil.getCurrentCountryIso(context);
        mPhoneNumberUtils = new PhoneNumberUtilsWrapper(context);

        // Enable all shortcuts by default
        for (int i = 0; i < mShortcutEnabled.length; i++) {
            mShortcutEnabled[i] = true;
        }

        initResources(context);
    }

    @Override
    public int getCount() {
        return super.getCount() + getShortcutCount();
    }

    /**
     * @return The number of enabled shortcuts. Ranges from 0 to a maximum of SHORTCUT_COUNT
     */
    public int getShortcutCount() {
        int count = 0;
        for (int i = 0; i < mShortcutEnabled.length; i++) {
            if (mShortcutEnabled[i]) count++;
        }
        return count;
    }

    @Override
    public int getItemViewType(int position) {
        final int shortcut = getShortcutTypeFromPosition(position);
        if (shortcut >= 0) {
            // shortcutPos should always range from 1 to SHORTCUT_COUNT
            return super.getViewTypeCount() + shortcut;
        } else {
            return super.getItemViewType(position);
        }
    }

    @Override
    public int getViewTypeCount() {
        // Number of item view types in the super implementation + 2 for the 2 new shortcuts
        return super.getViewTypeCount() + SHORTCUT_COUNT;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final int shortcutType = getShortcutTypeFromPosition(position);
        if (shortcutType >= 0) {
            if (convertView != null) {
                assignShortcutToView((ContactListItemView) convertView, shortcutType);
                return convertView;
            } else {
                final ContactListItemView v = new ContactListItemView(getContext(), null);
                assignShortcutToView(v, shortcutType);
                return v;
            }
        } else {
            return super.getView(position, convertView, parent);
        }
    }

    /**
     * @param position The position of the item
     * @return The enabled shortcut type matching the given position if the item is a
     * shortcut, -1 otherwise
     */
    public int getShortcutTypeFromPosition(int position) {
        int shortcutCount = position - super.getCount();
        if (shortcutCount >= 0) {
            // Iterate through the array of shortcuts, looking only for shortcuts where
            // mShortcutEnabled[i] is true
            for (int i = 0; shortcutCount >= 0 && i < mShortcutEnabled.length; i++) {
                if (mShortcutEnabled[i]) {
                    shortcutCount--;
                    if (shortcutCount < 0) return i;
                }
            }
            throw new IllegalArgumentException("Invalid position - greater than cursor count "
                    + " but not a shortcut.");
        }
        return SHORTCUT_INVALID;
    }

    @Override
    public boolean isEmpty() {
        return getShortcutCount() == 0 && super.isEmpty();
    }

    @Override
    public boolean isEnabled(int position) {
        final int shortcutType = getShortcutTypeFromPosition(position);
        if (shortcutType >= 0) {
            return true;
        } else {
            return super.isEnabled(position);
        }
    }

    private void assignShortcutToView(ContactListItemView v, int shortcutType) {
        final CharSequence text;
        final int drawableId;
        final Resources resources = getContext().getResources();
        final String number = getFormattedQueryString();
        switch (shortcutType) {
            case SHORTCUT_DIRECT_CALL:
                text = resources.getString(R.string.search_shortcut_call_number, number);
                drawableId = R.drawable.ic_search_phone;
                break;
            case SHORTCUT_ADD_NUMBER_TO_CONTACTS:
                text = resources.getString(R.string.search_shortcut_add_to_contacts);
                drawableId = R.drawable.ic_search_add_contact;
                break;
            case SHORTCUT_MAKE_VIDEO_CALL:
                text = resources.getString(R.string.search_shortcut_make_video_call);
                drawableId = R.drawable.ic_videocam;
                break;
            ///M: [VOLTE] For Volte Call @{
            case SHORTCUT_MAKE_VOLTE_CALL:
                text = getQueryString();
                drawableId = R.drawable.ic_search_phone;
                break;
            /// @}
            default:
                throw new IllegalArgumentException("Invalid shortcut type");
        }
        v.setDrawableResource(R.drawable.search_shortcut_background, drawableId);
        v.setDisplayName(text);
        v.setPhotoPosition(super.getPhotoPosition());
        v.setAdjustSelectionBoundsEnabled(false);
    }

    /**
     * @return True if the shortcut state (disabled vs enabled) was changed by this operation
     */
    public boolean setShortcutEnabled(int shortcutType, boolean visible) {
        final boolean changed = mShortcutEnabled[shortcutType] != visible;
        mShortcutEnabled[shortcutType] = visible;
        return changed;
    }

    public String getFormattedQueryString() {
        return mFormattedQueryString;
    }

    @Override
    public void setQueryString(String queryString) {
        mFormattedQueryString = PhoneNumberUtils.formatNumber(
                PhoneNumberUtils.normalizeNumber(queryString), mCountryIso);
        super.setQueryString(queryString);
    }

    @Override
    protected void bindView(View itemView, int partition, Cursor cursor, int position) {
        LogUtils.d(TAG, "---bindView---begin");

        final int viewType = getViewType(cursor);

        switch (viewType) {
            case VIEW_TYPE_CONTACT:
                bindContactView(itemView, getContext(), cursor);
                break;
            case VIEW_TYPE_CALL_LOG:
                bindCallLogView(itemView, getContext(), cursor);
                break;
            default:
                break;
        }
        LogUtils.d(TAG, "---bindView---end");
    }

    @Override
    protected View newView(Context context, int partition, Cursor cursor, int position, ViewGroup parent) {
        LogUtils.d(TAG, "---newView---begin");

        View view = View.inflate(context, R.layout.mtk_dialer_search_item_view, null);
        TypedArray a = context.obtainStyledAttributes(null, R.styleable.ContactListItemView);

        view.setPadding(a.getDimensionPixelOffset(
                R.styleable.ContactListItemView_list_item_padding_left, 0),
        a.getDimensionPixelOffset(
                R.styleable.ContactListItemView_list_item_padding_top, 0),
        a.getDimensionPixelOffset(
                R.styleable.ContactListItemView_list_item_padding_right, 0),
        a.getDimensionPixelOffset(
                R.styleable.ContactListItemView_list_item_padding_bottom, 0));

        ViewHolder viewHolder = createViewHolder();

        viewHolder.quickContactBadge = (QuickContactBadge) view.findViewById(R.id.quick_contact_photo);
        viewHolder.name = (TextView) view.findViewById(R.id.name);
        viewHolder.labelAndNumber = (TextView) view.findViewById(R.id.labelAndNumber);
        viewHolder.callInfo = (View) view.findViewById(R.id.call_info);
        viewHolder.callType = (ImageView) view.findViewById(R.id.callType);
        viewHolder.address = (TextView) view.findViewById(R.id.address);
        viewHolder.date = (TextView) view.findViewById(R.id.date);

        viewHolder.accountLabel = (TextView) view.findViewById(R.id.call_account_label);

        view.setTag(viewHolder);

        LogUtils.d(TAG, "---newView---end");
        return view;
    }

    public PhoneAccountHandle getSuggestPhoneAccountHandle(int position) {
        final Cursor cursor = (Cursor)getItem(position);
        PhoneAccountHandle phoneAccountHandle = null;
        if (cursor != null) {
            phoneAccountHandle = PhoneAccountUtils.getAccount(
                    cursor.getString(PHONE_ACCOUNT_COMPONENT_NAME_INDEX),
                    cursor.getString(PHONE_ACCOUNT_ID_INDEX));

            long id = cursor.getLong(DATA_ID_INDEX);

            if (id >= 0) {
                phoneAccountHandle = null;
            }
            return phoneAccountHandle;
        } else {
            Log.w(TAG, "Cursor was null in getPhoneAccountHandle() . Returning null instead.");
            return null;
        }
    }

    private void initResources(Context context) {

        mContactPhotoManager = ContactPhotoManager.getInstance(context);
        mPhoneNumberHelper = new PhoneNumberDisplayHelper(context, context.getResources());

        mEmergency = context.getResources().getString(R.string.emergencycall);
        mVoiceMail = context.getResources().getString(R.string.voicemail);
        mPrivateNumber = context.getResources().getString(R.string.private_num);
        mPayphoneNumber = context.getResources().getString(R.string.payphone);
        mUnknownNumber = context.getResources().getString(R.string.unknown);

        // 1. incoming 2. outgoing 3. missed 4.voicemail 5. autorejected(Calls.AUTOREJECTED_TYPE)
        // Align drawables of result items in dialer search to AOSP style.
        CallTypeIconsView.Resources resources = new CallTypeIconsView.Resources(context);
        mCallTypeDrawables[Calls.INCOMING_TYPE] = resources.incoming;
        mCallTypeDrawables[Calls.OUTGOING_TYPE] = resources.outgoing;
        mCallTypeDrawables[Calls.MISSED_TYPE] = resources.missed;
        mCallTypeDrawables[Calls.VOICEMAIL_TYPE] = resources.voicemail;
        mCallTypeDrawables[Calls.AUTO_REJECT_TYPE] = resources.autorejected;
    }

    private int getViewType(Cursor cursor) {
        int retval = VIEW_TYPE_UNKNOWN;
        final int contactId = cursor.getInt(CONTACT_ID_INDEX);
        final int callLogId = cursor.getInt(CALL_LOG_ID_INDEX);

        LogUtils.d(TAG, "getViewType: contactId: " + contactId + " ,callLogId: " + callLogId);

        if (contactId > 0) {
            retval = VIEW_TYPE_CONTACT;
        } else if (callLogId > 0) {
            retval = VIEW_TYPE_CALL_LOG;
        }

        return retval;
    }

    private void bindContactView(View view, Context context, Cursor cursor) {

        final ViewHolder viewHolder = (ViewHolder) view.getTag();

        viewHolder.labelAndNumber.setVisibility(View.VISIBLE);
        viewHolder.callInfo.setVisibility(View.GONE);
        viewHolder.accountLabel.setVisibility(View.GONE);

        final String number = cursor.getString(SEARCH_PHONE_NUMBER_INDEX);
        String formatNumber = numberLeftToRight(PhoneNumberUtils.formatNumber(number, mCountryIso));
        if (formatNumber == null) {
            formatNumber = number;
        }

        final int presentation = cursor.getInt(PRESENTATION_INDEX);
        final PhoneAccountHandle accountHandle = PhoneAccountUtils.getAccount(
                cursor.getString(PHONE_ACCOUNT_COMPONENT_NAME_INDEX),
                cursor.getString(PHONE_ACCOUNT_ID_INDEX));

        final int numberType = getNumberType(accountHandle, number, presentation);

        final int labelType = cursor.getInt(SEARCH_PHONE_TYPE_INDEX);
        /// M: for plug-in @{
       // CharSequence label = CommonDataKinds.Phone.getTypeLabel(context.getResources(), labelType, null);
        CharSequence label = cursor.getString(SEARCH_PHONE_LABEL_INDEX);
        ICallerInfoExt callerInfoExt = (ICallerInfoExt) MPlugin.createInstance(ICallerInfoExt.class.getName(), mContext);
        int subId = cursor.getInt(INDICATE_PHONE_SIM_INDEX);
        if (callerInfoExt != null) {
            label = callerInfoExt.getTypeLabel(context, labelType, label, null, subId);
        }
        /// @}

        final CharSequence displayName = cursor.getString(NAME_INDEX);

        LogUtils.d(TAG, "bindContactView. displayName = " + displayName + " number = " + number + " label = " + label + " subId: " + subId);

        Uri contactUri = getContactUri(cursor);
        LogUtils.d(TAG, "bindContactView, contactUri: " + contactUri);

        long photoId = cursor.getLong(PHOTO_ID_INDEX);

        if (numberType == NUMBER_TYPE_VOICEMAIL || numberType == NUMBER_TYPE_EMERGENCY) {
            photoId = 0;
            viewHolder.quickContactBadge.assignContactUri(null);
        } else {
            viewHolder.quickContactBadge.assignContactUri(contactUri);
        }
        viewHolder.quickContactBadge.setOverlay(null);

        if (photoId > 0) {
            mContactPhotoManager.loadThumbnail(viewHolder.quickContactBadge, photoId, false, true, null);
        } else {
            String identifier = cursor.getString(CONTACT_NAME_LOOKUP_INDEX);
            DefaultImageRequest request = new DefaultImageRequest((String)displayName, identifier, true);
            if (subId > 0) {
                request.subId = subId;
                request.photoId = cursor.getInt(IS_SDN_CONTACT);
            }
            mContactPhotoManager.loadThumbnail(viewHolder.quickContactBadge, photoId, false, true, request);
        }

        if (isSpecialNumber(numberType)) {
            if (numberType == NUMBER_TYPE_VOICEMAIL || numberType == NUMBER_TYPE_EMERGENCY) {
                if (numberType == NUMBER_TYPE_VOICEMAIL) {
                    viewHolder.name.setText(mVoiceMail);
                } else {
                    viewHolder.name.setText(mEmergency);
                }

                viewHolder.labelAndNumber.setVisibility(View.VISIBLE);
                String highlight = getNumberHighlight(cursor);
                if (!TextUtils.isEmpty(highlight)) {
                    SpannableStringBuilder style = highlightHyphon(highlight, formatNumber, number);
                    viewHolder.labelAndNumber.setText(style);
               } else {
                   viewHolder.labelAndNumber.setText(formatNumber);
                }
            } else {
                final String convert = specialNumberToString(numberType);
                viewHolder.name.setText(convert);
            }
        } else {
            // empty name ?
            if (!TextUtils.isEmpty(displayName)) {
                // highlight name
                String highlight = getNameHighlight(cursor);
                if (!TextUtils.isEmpty(highlight)) {
                    SpannableStringBuilder style = highlightString(highlight, displayName);
                    viewHolder.name.setText(style);
                    if (isRegularSearch(cursor)) {
                        viewHolder.name.setText(highlightName(highlight, displayName));
                    }
                } else {
                    viewHolder.name.setText(displayName);
                }
                // highlight number
                if (!TextUtils.isEmpty(formatNumber)) {
                    highlight = getNumberHighlight(cursor);
                    if (!TextUtils.isEmpty(highlight)) {
                        SpannableStringBuilder style = highlightHyphon(highlight, formatNumber, number);
                        setLabelAndNumber(viewHolder.labelAndNumber, label, style);
                    } else {
                        setLabelAndNumber(viewHolder.labelAndNumber, label, formatNumber);
                   }
                } else {
                    viewHolder.labelAndNumber.setVisibility(View.GONE);
                }
            } else {
                viewHolder.labelAndNumber.setVisibility(View.GONE);

                // highlight number and set number to name text view
                if (!TextUtils.isEmpty(formatNumber)) {
                    final String highlight = getNumberHighlight(cursor);
                    if (!TextUtils.isEmpty(highlight)) {
                        SpannableStringBuilder style = highlightHyphon(highlight, formatNumber, number);
                        viewHolder.name.setText(style);
                    } else {
                        viewHolder.name.setText(formatNumber);
                    }
                } else {
                    viewHolder.name.setVisibility(View.GONE);
                }
            }
        }

        /// M: for plug-in @{
        ExtensionManager.getInstance().getDialerSearchExtension().setCallAccountForDialerSearch(context, view, null);
        /// @}
    }

    private void bindCallLogView(View view, Context context, Cursor cursor) {
        final ViewHolder viewHolder = (ViewHolder) view.getTag();

        viewHolder.callInfo.setVisibility(View.VISIBLE);
        viewHolder.labelAndNumber.setVisibility(View.GONE);

        final String number = cursor.getString(SEARCH_PHONE_NUMBER_INDEX);
        String formattedNumber = numberLeftToRight(PhoneNumberUtils.formatNumber(number, mCountryIso));
        if (TextUtils.isEmpty(formattedNumber)) {
            formattedNumber = number;
        }

        final int presentation = cursor.getInt(PRESENTATION_INDEX);
        final PhoneAccountHandle accountHandle = PhoneAccountUtils.getAccount(
                cursor.getString(PHONE_ACCOUNT_COMPONENT_NAME_INDEX),
                cursor.getString(PHONE_ACCOUNT_ID_INDEX));

        final int numberType = getNumberType(accountHandle, number, presentation);

        final int type = cursor.getInt(CALL_TYPE_INDEX);
       // final int simId = cursor.getInt(SIM_ID_INDEX);
        final long date = cursor.getLong(CALL_LOG_DATE_INDEX);
        final int indicate = cursor.getInt(INDICATE_PHONE_SIM_INDEX);
        //qianweiqiang add boway for location hide 2015 7 15
        String geocode = cursor.getString(CALL_GEOCODED_LOCATION_INDEX);
        final SharedPreferences prefs = mContext.getSharedPreferences(
                DialtactsActivity.SHARED_PREFS_NAME, Context.MODE_WORLD_WRITEABLE);
        Boolean isOpen = prefs.getBoolean("IS_SHOW", false);
        if(!isOpen) {
            geocode = "";
        }
        //qianweiqiang add boway for location hide 2015 7 15
        
        /** M: [Union Query] create a temp contact uri for quick contact view. @{ */
        Uri contactUri = null;
        if (DialerFeatureOptions.CALL_LOG_UNION_QUERY && !TextUtils.isEmpty(number)) {
            contactUri = ContactInfoHelper.createTemporaryContactUri(number);
        }
        /** @} */

        int contactType = ContactPhotoManager.TYPE_DEFAULT;
        if (numberType == NUMBER_TYPE_VOICEMAIL) {
            contactType = ContactPhotoManager.TYPE_VOICEMAIL;
            contactUri = null;
        }

        viewHolder.quickContactBadge.assignContactUri(contactUri);
        viewHolder.quickContactBadge.setOverlay(null);

        /// M: [ALPS01963857] keep call log and smart search's avatar in same color
        String nameForDefaultImage = mPhoneNumberHelper.getDisplayNumber(accountHandle,
                    number, presentation, number).toString();

        String identifier = cursor.getString(CONTACT_NAME_LOOKUP_INDEX);
        DefaultImageRequest request = new DefaultImageRequest(nameForDefaultImage, identifier, contactType, true);
        mContactPhotoManager.loadThumbnail(viewHolder.quickContactBadge, 0, false, true, request);

        if (TextUtils.isEmpty(geocode)) {
            geocode = context.getResources().getString(R.string.call_log_empty_geocode);
        }

        viewHolder.address.setText(geocode);

        if (isSpecialNumber(numberType)) {
            if (numberType == NUMBER_TYPE_VOICEMAIL || numberType == NUMBER_TYPE_EMERGENCY) {
                if (numberType == NUMBER_TYPE_VOICEMAIL) {
                    viewHolder.name.setText(mVoiceMail);
                } else {
                    viewHolder.name.setText(mEmergency);
                }

                String highlight = getNumberHighlight(cursor);
                if (!TextUtils.isEmpty(highlight)) {
                    SpannableStringBuilder style = highlightHyphon(highlight, formattedNumber, number);
                    viewHolder.address.setText(style);
                } else {
                    viewHolder.address.setText(formattedNumber);
                }
            } else {
                final String convert = specialNumberToString(numberType);
                viewHolder.name.setText(convert);
            }
        } else {
            if (!TextUtils.isEmpty(formattedNumber)) {
                String highlight = getNumberHighlight(cursor);
                if (!TextUtils.isEmpty(highlight)) {
                    SpannableStringBuilder style = highlightHyphon(highlight, formattedNumber, number);
                    viewHolder.name.setText(style);
                } else {
                    viewHolder.name.setText(formattedNumber);
                }
            }
        }

        java.text.DateFormat dateFormat = DateFormat.getTimeFormat(context);
        String dateString = dateFormat.format(date);
        viewHolder.date.setText(dateString);

        Drawable[] callTypeDrawables = mCallTypeDrawables;
        viewHolder.callType.setImageDrawable(callTypeDrawables[type]);

        final String accountLabel = PhoneAccountUtils.getAccountLabel(context, accountHandle);

        if (!TextUtils.isEmpty(accountLabel)) {
            viewHolder.accountLabel.setVisibility(View.VISIBLE);
            viewHolder.accountLabel.setText(accountLabel);
            // Set text color for the corresponding account.
            int color = PhoneAccountUtils.getAccountColor(context, accountHandle);
            if (color == PhoneAccount.NO_HIGHLIGHT_COLOR) {
                int defaultColor = R.color.dialtacts_secondary_text_color;
                viewHolder.accountLabel.setTextColor(context.getResources().getColor(defaultColor));
            } else {
                viewHolder.accountLabel.setTextColor(color);
            }
        } else {
            viewHolder.accountLabel.setVisibility(View.GONE);
        }

        /// M: add for plug-in @{
        ExtensionManager.getInstance().getDialerSearchExtension().setCallAccountForDialerSearch(context, view, accountHandle);
        /// @}
    }

    private int getNumberType(PhoneAccountHandle accountHandle, CharSequence number,
            int presentation) {
        int type = NUMBER_TYPE_NORMAL;
        if (presentation == Calls.PRESENTATION_UNKNOWN) {
            type = NUMBER_TYPE_UNKNOWN;
        } else if (presentation == Calls.PRESENTATION_RESTRICTED) {
            type = NUMBER_TYPE_PRIVATE;
        } else if (presentation == Calls.PRESENTATION_PAYPHONE) {
            type = NUMBER_TYPE_PAYPHONE;
        } else if (mPhoneNumberUtils.isVoicemailNumber(accountHandle, number)) {
           type = NUMBER_TYPE_VOICEMAIL;
        }
        if (PhoneNumberUtilsWrapper.isLegacyUnknownNumbers(number)) {
            type = NUMBER_TYPE_UNKNOWN;
        }
        return type;
    }

    private Uri getContactUri(Cursor cursor) {
        final String lookup = cursor.getString(CONTACT_NAME_LOOKUP_INDEX);
        final int contactId = cursor.getInt(CONTACT_ID_INDEX);
        return Contacts.getLookupUri(contactId, lookup);
    }

    private boolean isSpecialNumber(int type) {
        return type != NUMBER_TYPE_NORMAL;
    }

    private SpannableStringBuilder highlightString(String highlight, CharSequence target) {
        SpannableStringBuilder style = new SpannableStringBuilder(target);
        int length = highlight.length();
        for (int i = DS_MATCHED_DATA_INIT_POS; i + 1 < length; i += DS_MATCHED_DATA_DIVIDER) {
            if (((int) highlight.charAt(i)) > style.length()
                    || ((int) highlight.charAt(i + 1) + 1) > style.length()) {
                break;
            }
            style.setSpan(new StyleSpan(Typeface.BOLD), (int) highlight.charAt(i),
                    (int) highlight.charAt(i + 1) + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return style;
    }

    private CharSequence highlightName(String highlight, CharSequence target) {
        String highlightedPrefix = getUpperCaseQueryString();
        if (highlightedPrefix != null) {
            mTextHighlighter = new TextHighlighter(Typeface.BOLD);
            target =  mTextHighlighter.applyPrefixHighlight(target, highlightedPrefix);
        }
        return target;
    }

    private SpannableStringBuilder highlightHyphon(String highlight, String target, String origin) {
        if (target == null) {
            Log.w(TAG, "[highlightHyphon] target is null");
            return null;
        }
        SpannableStringBuilder style = new SpannableStringBuilder(target);
        ArrayList<Integer> numberHighlightOffset = DialerSearchUtils
                .adjustHighlitePositionForHyphen(target, highlight.substring(DS_MATCHED_DATA_INIT_POS), origin);
        if (numberHighlightOffset != null && numberHighlightOffset.size() > 1) {
            style.setSpan(new StyleSpan(Typeface.BOLD),
                    numberHighlightOffset.get(0),
                    numberHighlightOffset.get(1) + 1,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return style;
    }

    private String getNameHighlight(Cursor cursor) {
        final int index = cursor.getColumnIndex(DialerSearch.MATCHED_NAME_OFFSET);
        return index != -1 ? cursor.getString(index) : null;
    }

    private boolean isRegularSearch(Cursor cursor) {
        final int index = cursor.getColumnIndex(DialerSearch.MATCHED_DATA_OFFSET);
        String regularSearch = (index != -1 ? cursor.getString(index) : null);
        LogUtils.d(TAG, "" + regularSearch);

        return Boolean.valueOf(regularSearch);
    }

    private String getNumberHighlight(Cursor cursor) {
        final int index = cursor.getColumnIndex(DialerSearch.MATCHED_DATA_OFFSET);
        return index != -1 ? cursor.getString(index) : null;
    }

    private void setLabelAndNumber(TextView view, CharSequence label, SpannableStringBuilder number) {
        if (PhoneNumberUtils.isUriNumber(number.toString())) {
            view.setText(number);
            return;
        }
        if (TextUtils.isEmpty(label)) {
            view.setText(number);
        } else if (TextUtils.isEmpty(number)) {
            view.setText(label);
        } else {
            number.insert(0, label + " ");
            view.setText(number);
        }
    }

    private void setLabelAndNumber(TextView view, CharSequence label, String number) {
        if (PhoneNumberUtils.isUriNumber(number)) {
            view.setText(number);
            return;
        }

        if (TextUtils.isEmpty(label)) {
            view.setText(number);
        } else if (TextUtils.isEmpty(number)) {
            view.setText(label);
        } else {
            view.setText(label + " " + number);
        }
    }

    private String specialNumberToString(int type) {
        switch (type) {
            case NUMBER_TYPE_UNKNOWN:
                return mUnknownNumber;
            case NUMBER_TYPE_PRIVATE:
                return mPrivateNumber;
            case NUMBER_TYPE_PAYPHONE:
                return mPayphoneNumber;
            default:
                break;
        }
        return null;
    }

    private class ViewHolder {
        public QuickContactBadge quickContactBadge;
        public TextView name;
        public TextView labelAndNumber;
        public View callInfo;
        public ImageView callType;
        public TextView address;
        public TextView date;
        public TextView accountLabel;
    }

    private ViewHolder createViewHolder() {
        final ViewHolder viewHolder = new ViewHolder();
        return viewHolder;
    }

    /// M: Fix CR: ALPS01398152, Support RTL display for Arabic/Hebrew/Urdu @{
    private String numberLeftToRight(String origin) {
        return TextUtils.isEmpty(origin) ? origin : '\u202D' + origin + '\u202C';
    }
    /// M: @}
}
