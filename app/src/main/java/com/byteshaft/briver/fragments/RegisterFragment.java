package com.byteshaft.briver.fragments;

import android.app.Activity;
import android.app.Dialog;
import android.app.Fragment;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.telephony.PhoneNumberUtils;
import android.text.SpannableStringBuilder;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.byteshaft.briver.R;
import com.byteshaft.briver.utils.AppGlobals;
import com.byteshaft.briver.utils.EndPoints;
import com.byteshaft.briver.utils.Helpers;
import com.byteshaft.briver.utils.LocationService;
import com.byteshaft.briver.utils.MultipartDataUtility;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by fi8er1 on 28/04/2016.
 */
public class RegisterFragment extends Fragment implements View.OnClickListener, CompoundButton.OnCheckedChangeListener {

    public static LatLng latLngDriverLocationForRegistration;
    public static boolean isRegistrationFragmentOpen;
    public static int responseCode;
    public static View baseViewRegisterFragment;
    public static boolean locationAcquired;
    static String userRegisterEmail;
    static int registerUserType = -1;
    static String driverLocationToString;
    final Runnable closeRegistration = new Runnable() {
        public void run() {
            getActivity().onBackPressed();
            locationAcquired = false;
        }
    };
    final Runnable openLocationServiceSettings = new Runnable() {
        public void run() {
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(intent);
        }
    };
    final Runnable driverRegistrationContinueAnyway = new Runnable() {
        public void run() {
            new RegisterUserTask().execute();
        }
    };
    final private int CAPTURE_IMAGE = 1;
    final Runnable openCameraIntent = new Runnable() {
        public void run() {
            Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
            File file = new File(Environment.getExternalStorageDirectory() + File.separator +
                    "image.jpg");
            intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(file));
            startActivityForResult(intent, CAPTURE_IMAGE);
        }
    };
    final private int PICK_IMAGE = 2;
    final Runnable openGalleryIntent = new Runnable() {
        public void run() {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);//
            startActivityForResult(Intent.createChooser(intent, "Select File"), PICK_IMAGE);
        }
    };
    HashMap<Integer, String> hashMap;
    ImageButton ibPhotoOne;
    ImageButton ibPhotoTwo;
    ImageButton ibPhotoThree;
    int ibPosition;
    EditText etRegisterUserFullName;
    EditText etRegisterUserEmail;
    EditText etRegisterUserEmailRepeat;
    EditText etRegisterUserPassword;
    EditText etRegisterUserConfirmPassword;
    EditText etRegisterUserContactNumber;
    EditText etRegisterUserDrivingExperience;
    EditText etRegisterUserBasicBio;
    EditText etRegisterUserVehicleMake;
    EditText etRegisterUserVehicleModel;
    EditText etRegisterAttachments;
    LinearLayout llRegisterElements;
    LinearLayout llRegisterElementsDriver;
    LinearLayout llRegisterElementsCustomer;
    RadioGroup rgRegisterSelectUserType;
    RadioGroup rgRegisterCustomerSelectTransmissionType;
    RadioGroup rgRegisterDriverSelectTransmissionType;
    RadioButton rbRegisterCustomer;
    RadioButton rbRegisterDriver;
    RadioButton rbVehicleTypeMini;
    RadioButton rbVehicleTypeHatchback;
    RadioButton rbVehicleTypeSedan;
    RadioButton rbVehicleTypeLuxury;
    CheckBox cbTermsOfServiceCheck;
    String userRegisterFullName;
    String userRegisterEmailRepeat;
    String userRegisterPassword;
    String userRegisterConfirmPassword;
    String userRegisterContactNumber;
    String userRegisterDrivingExperience;
    String userRegisterBasicBio;
    String userRegisterVehicleMake;
    String userRegisterVehicleModel;
    int userRegisterVehicleType = -1;
    int transmissionType = -1;
    HttpURLConnection connection;
    LocationService mLocationService;
    final Runnable recheckLocationServiceStatus = new Runnable() {
        public void run() {
            if (Helpers.isAnyLocationServiceAvailable()) {
                mLocationService.startLocationServices();
            } else {
                Helpers.AlertDialogWithPositiveNegativeNeutralFunctions(getActivity(), "Location Service disabled",
                        "Enable device GPS to continue driver registration", "Settings", "Exit", "Re-Check",
                        openLocationServiceSettings, closeRegistration, recheckLocationServiceStatus);
            }
        }
    };
    Button btnCreateUser;
    RegisterUserTask taskRegisterUser;
    boolean isUserRegistrationTaskRunning;
    private ArrayList<HashMap<Integer, String>> imagePathsArray;

    public static String getRegistrationStringForCustomer(
            String fullName, String email, String password, String phone, int transmissionType, int vehicleType,
            String vehicleMake, String vehicleModel) {

        JSONObject json = new JSONObject();
        try {
            json.put("full_name", fullName);
            json.put("email", email);
            json.put("password", password);
            json.put("phone_number", phone);
            json.put("transmission_type", transmissionType);
            json.put("vehicle_type", vehicleType);
            json.put("vehicle_make", vehicleMake);
            json.put("vehicle_model", vehicleModel);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json.toString();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        baseViewRegisterFragment = inflater.inflate(R.layout.fragment_register, container, false);

        mLocationService = new LocationService(getActivity());

        etRegisterUserFullName = (EditText) baseViewRegisterFragment.findViewById(R.id.et_register_full_name);
        etRegisterUserEmail = (EditText) baseViewRegisterFragment.findViewById(R.id.et_register_email);
        etRegisterUserEmailRepeat = (EditText) baseViewRegisterFragment.findViewById(R.id.et_register_email_repeat);
        etRegisterUserPassword = (EditText) baseViewRegisterFragment.findViewById(R.id.et_register_password);
        etRegisterUserConfirmPassword = (EditText) baseViewRegisterFragment.findViewById(R.id.et_register_confirm_password);
        etRegisterUserContactNumber = (EditText) baseViewRegisterFragment.findViewById(R.id.et_register_phone_number);
        etRegisterUserDrivingExperience = (EditText) baseViewRegisterFragment.findViewById(R.id.et_register_driving_experience);
        etRegisterUserBasicBio = (EditText) baseViewRegisterFragment.findViewById(R.id.et_register_bio);
        etRegisterUserVehicleMake = (EditText) baseViewRegisterFragment.findViewById(R.id.et_register_vehicle_make);
        etRegisterUserVehicleModel = (EditText) baseViewRegisterFragment.findViewById(R.id.et_register_vehicle_model);
        etRegisterAttachments = (EditText) baseViewRegisterFragment.findViewById(R.id.et_register_attachments);

        cbTermsOfServiceCheck = (CheckBox) baseViewRegisterFragment.findViewById(R.id.cb_terms_of_service_check);

        baseViewRegisterFragment.findViewById(R.id.ll_register).requestFocus();
        rgRegisterSelectUserType = (RadioGroup) baseViewRegisterFragment.findViewById(R.id.rg_register_select_user_type);
        rgRegisterCustomerSelectTransmissionType = (RadioGroup) baseViewRegisterFragment.findViewById(R.id.rg_register_customer_select_transmission_type);
        rgRegisterDriverSelectTransmissionType = (RadioGroup) baseViewRegisterFragment.findViewById(R.id.rg_register_driver_select_transmission_type);
        rbRegisterCustomer = (RadioButton) baseViewRegisterFragment.findViewById(R.id.rb_register_customer);
        rbRegisterDriver = (RadioButton) baseViewRegisterFragment.findViewById(R.id.rb_register_driver);
        rbVehicleTypeMini = (RadioButton) baseViewRegisterFragment.findViewById(R.id.rb_register_customer_vehicle_type_mini);
        rbVehicleTypeHatchback = (RadioButton) baseViewRegisterFragment.findViewById(R.id.rb_register_customer_vehicle_type_hatchback);
        rbVehicleTypeSedan = (RadioButton) baseViewRegisterFragment.findViewById(R.id.rb_register_customer_vehicle_type_sedan);
        rbVehicleTypeLuxury = (RadioButton) baseViewRegisterFragment.findViewById(R.id.rb_register_customer_vehicle_type_luxury);

        rbVehicleTypeMini.setOnCheckedChangeListener(this);
        rbVehicleTypeHatchback.setOnCheckedChangeListener(this);
        rbVehicleTypeSedan.setOnCheckedChangeListener(this);
        rbVehicleTypeLuxury.setOnCheckedChangeListener(this);
        etRegisterAttachments.setOnClickListener(this);

        btnCreateUser = (Button) baseViewRegisterFragment.findViewById(R.id.btn_register_create_account);
        btnCreateUser.setOnClickListener(this);
        hashMap = new HashMap<>();
        imagePathsArray = new ArrayList<>();

        llRegisterElements = (LinearLayout) baseViewRegisterFragment.findViewById(R.id.layout_elements_register);
        llRegisterElementsCustomer = (LinearLayout) baseViewRegisterFragment.findViewById(R.id.layout_elements_register_customer);
        llRegisterElementsDriver = (LinearLayout) baseViewRegisterFragment.findViewById(R.id.layout_elements_register_driver);

        rgRegisterSelectUserType.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.rb_register_customer) {
                    rbRegisterDriver.setVisibility(View.INVISIBLE);
                    llRegisterElements.setVisibility(View.VISIBLE);
                    llRegisterElementsCustomer.setVisibility(View.VISIBLE);
                    registerUserType = 0;
                    rbVehicleTypeSedan.setChecked(true);
                    rgRegisterCustomerSelectTransmissionType.check(R.id.rb_register_customer_transmission_type_manual);
                } else if (checkedId == R.id.rb_register_driver) {
                    rbRegisterCustomer.setVisibility(View.INVISIBLE);
                    llRegisterElements.setVisibility(View.VISIBLE);
                    llRegisterElementsDriver.setVisibility(View.VISIBLE);
                    registerUserType = 1;
                    rgRegisterDriverSelectTransmissionType.check(R.id.rb_register_driver_transmission_type_both);
                    if (AppGlobals.checkPlayServicesAvailability()) {
                        if (Helpers.isAnyLocationServiceAvailable()) {
                            mLocationService.startLocationServices();
                            Helpers.showSnackBar(baseViewRegisterFragment, "Acquiring location for registration", Snackbar.LENGTH_SHORT, "#ffffff");
                        } else {
                            Helpers.AlertDialogWithPositiveNegativeNeutralFunctions(getActivity(), "Location Service disabled",
                                    "Enable device GPS to continue driver registration", "Settings", "Exit", "Re-Check",
                                    openLocationServiceSettings, closeRegistration, recheckLocationServiceStatus);
                        }

                    }
                }
            }
        });

        rgRegisterCustomerSelectTransmissionType.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.rb_register_customer_transmission_type_manual) {
                    transmissionType = 0;
                }
                if (checkedId == R.id.rb_register_customer_transmission_type_auto) {
                    transmissionType = 1;
                }
            }
        });

        rgRegisterDriverSelectTransmissionType.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.rb_register_driver_transmission_type_manual) {
                    transmissionType = 0;
                } else if (checkedId == R.id.rb_register_driver_transmission_type_auto) {
                    transmissionType = 1;
                } else if (checkedId == R.id.rb_register_driver_transmission_type_both) {
                    transmissionType = 2;
                }
            }
        });

        SpannableStringBuilder text = new SpannableStringBuilder();
        text.append(getString(R.string.TermsOfServiceInitialText)).append(" ");

        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(View view) {
                Helpers.AlertDialogMessage(getActivity(), "Terms of Service", "This is the dummy Terms of Service", "Dismiss");
            }
        };
        TextPaint ds = new TextPaint();
        clickableSpan.updateDrawState(ds);
        ds.setUnderlineText(false);
        text.append(getString(R.string.TermsOfServiceLateralText));

        text.setSpan(clickableSpan, getString(R.string.TermsOfServiceInitialText).length() + 1,
                getString(R.string.TermsOfServiceInitialText).length() + 1 + getString(R.string.TermsOfServiceLateralText).length(), 0);
        cbTermsOfServiceCheck.setMovementMethod(LinkMovementMethod.getInstance());
        cbTermsOfServiceCheck.setText(text, TextView.BufferType.SPANNABLE);
        return baseViewRegisterFragment;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_register_create_account:

                userRegisterFullName = etRegisterUserFullName.getText().toString();
                userRegisterEmail = etRegisterUserEmail.getText().toString();
                userRegisterEmailRepeat = etRegisterUserEmailRepeat.getText().toString();
                userRegisterPassword = etRegisterUserPassword.getText().toString();
                userRegisterConfirmPassword = etRegisterUserConfirmPassword.getText().toString();
                userRegisterContactNumber = etRegisterUserContactNumber.getText().toString();
                userRegisterDrivingExperience = etRegisterUserDrivingExperience.getText().toString();
                userRegisterBasicBio = etRegisterUserBasicBio.getText().toString();
                userRegisterVehicleMake = etRegisterUserVehicleMake.getText().toString();
                userRegisterVehicleModel = etRegisterUserVehicleModel.getText().toString();

                if (validateRegisterInfo()) {
                    if (registerUserType == 1) {
                        if (latLngDriverLocationForRegistration != null) {
                            driverLocationToString = latLngDriverLocationForRegistration.latitude + "," + latLngDriverLocationForRegistration.longitude;
                            taskRegisterUser = (RegisterUserTask) new RegisterUserTask().execute();
                        } else {
                            driverLocationToString = null;
                            Helpers.AlertDialogWithPositiveFunctionNegativeButton(getActivity(), "Location Unavailable",
                                    "Your location is being acquired. You can either wait or it can be acquired later",
                                    "Continue Anyway", "Wait", driverRegistrationContinueAnyway);
                        }
                    } else {
                        new RegisterUserTask().execute();
                    }
                }
                break;
            case R.id.et_register_attachments:
                showDocumentsAttachmentCustomDialog();
                etRegisterAttachments.setError(null);
                break;
        }
    }

    public void loadFragment(Fragment fragment) {
        android.app.FragmentTransaction tx = getFragmentManager().beginTransaction();
        tx.setCustomAnimations(R.animator.anim_transition_fragment_slide_right_enter, R.animator.anim_transition_fragment_slide_left_exit,
                R.animator.anim_transition_fragment_slide_left_enter, R.animator.anim_transition_fragment_slide_right_exit);
        tx.replace(R.id.container, fragment).addToBackStack("Confirmation");
        tx.commit();
    }

    public boolean validateRegisterInfo() {
        boolean valid = true;

        if (userRegisterFullName.trim().isEmpty()) {
            etRegisterUserFullName.setError("Empty");
            valid = false;
        }

        if (userRegisterEmail.trim().isEmpty()) {
            etRegisterUserEmail.setError("Empty");
            valid = false;
        } else if (!userRegisterEmail.trim().isEmpty() && !android.util.Patterns.EMAIL_ADDRESS.matcher(userRegisterEmail).matches()) {
            etRegisterUserEmail.setError("Invalid E-Mail");
            valid = false;
        } else if (!userRegisterEmail.equals(userRegisterEmailRepeat)) {
            etRegisterUserEmailRepeat.setError("E-Mail doesn't match");
            valid = false;
        } else {
            etRegisterUserEmail.setError(null);
        }

        if (userRegisterPassword.trim().isEmpty() || userRegisterPassword.length() < 6) {
            etRegisterUserPassword.setError("Minimum 6 Characters");
            valid = false;
        } else if (!userRegisterPassword.equals(userRegisterConfirmPassword)) {
            etRegisterUserConfirmPassword.setError("Password doesn't match");
            valid = false;
        } else {
            etRegisterUserPassword.setError(null);
        }

        if (userRegisterContactNumber.trim().isEmpty()) {
            etRegisterUserContactNumber.setError("Empty");
            valid = false;
        } else if (!userRegisterContactNumber.isEmpty() && !PhoneNumberUtils.isGlobalPhoneNumber(userRegisterContactNumber)) {
            etRegisterUserContactNumber.setError("Number is invalid");
            valid = false;
        } else {
            etRegisterUserContactNumber.setError(null);
        }

        if (registerUserType == 0) {
            if (userRegisterVehicleMake.isEmpty() || userRegisterVehicleMake.length() < 3) {
                etRegisterUserVehicleMake.setError("Minimum 3 Characters");
                valid = false;
            } else {
                etRegisterUserVehicleMake.setError(null);
            }

            if (userRegisterVehicleModel.isEmpty()) {
                etRegisterUserVehicleModel.setError("Empty");
                valid = false;
            } else if (userRegisterVehicleModel.length() < 4) {
                etRegisterUserVehicleModel.setError("Minimum 4 Characters");
                valid = false;
            }
        }

        if (registerUserType == 1) {
            if (userRegisterDrivingExperience.trim().isEmpty()) {
                etRegisterUserDrivingExperience.setError("Empty");
                valid = false;
            } else {
                etRegisterUserDrivingExperience.setError(null);
            }

            if (!etRegisterAttachments.getText().toString().equals("Documents 3/3")) {
                etRegisterAttachments.setError("Attach documents");
                valid = false;
            } else {
                etRegisterAttachments.setError(null);
            }
        }

        if (valid && !cbTermsOfServiceCheck.isChecked()) {
            Helpers.showSnackBar(getView(), "Check terms of service to continue", Snackbar.LENGTH_LONG, "#f44336");
            valid = false;
        }

        return valid;
    }

    @Override
    public void onPause() {
        super.onPause();
        isRegistrationFragmentOpen = false;
        if (mLocationService.mGoogleApiClient != null && mLocationService.mGoogleApiClient.isConnected()) {
            mLocationService.stopLocationService();
        }
        if (isUserRegistrationTaskRunning) {
            taskRegisterUser.cancel(true);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        isRegistrationFragmentOpen = true;
        if (!locationAcquired && rgRegisterSelectUserType.getCheckedRadioButtonId() == R.id.rb_register_driver) {
            if (Helpers.isAnyLocationServiceAvailable()) {
                mLocationService.startLocationServices();
                Helpers.showSnackBar(baseViewRegisterFragment, "Acquiring location for registration", Snackbar.LENGTH_SHORT, "#ffffff");
            } else {
                Helpers.AlertDialogWithPositiveNegativeNeutralFunctions(getActivity(), "Location Service disabled",
                        "Enable device GPS to continue driver registration", "Settings", "Exit", "Re-Check",
                        openLocationServiceSettings, closeRegistration, recheckLocationServiceStatus);
            }
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        rbVehicleTypeMini.setChecked(false);
        rbVehicleTypeHatchback.setChecked(false);
        rbVehicleTypeSedan.setChecked(false);
        rbVehicleTypeLuxury.setChecked(false);
        switch (buttonView.getId()) {
            case R.id.rb_register_customer_vehicle_type_mini:
                userRegisterVehicleType = 0;
                rbVehicleTypeMini.setChecked(isChecked);
                break;
            case R.id.rb_register_customer_vehicle_type_hatchback:
                userRegisterVehicleType = 1;
                rbVehicleTypeHatchback.setChecked(isChecked);
                break;
            case R.id.rb_register_customer_vehicle_type_sedan:
                userRegisterVehicleType = 2;
                rbVehicleTypeSedan.setChecked(isChecked);
                break;
            case R.id.rb_register_customer_vehicle_type_luxury:
                userRegisterVehicleType = 3;
                rbVehicleTypeLuxury.setChecked(isChecked);
                break;
        }
    }

    public void onRegistrationSuccess() {
        Toast.makeText(getActivity(), "Registration successful", Toast.LENGTH_SHORT).show();
        Helpers.closeSoftKeyboard(getActivity());
        loadFragment(new CodeConfirmationFragment());
        CodeConfirmationFragment.isFragmentOpenedFromLogin = false;
    }

    public void onRegistrationFailed(String message) {
        Helpers.showSnackBar(baseViewRegisterFragment, message, Snackbar.LENGTH_SHORT, "#f44336");
    }

    private void showDocumentsAttachmentCustomDialog() {
        final Dialog customAttachmentsDialog = new Dialog(getActivity());
        customAttachmentsDialog.setContentView(R.layout.layout_custom_attachment_dialog);

        customAttachmentsDialog.setCancelable(false);
        customAttachmentsDialog.setTitle("Attach Documents");

        Button buttonNo = (Button) customAttachmentsDialog.findViewById(R.id.btn_driver_hire_dialog_cancel);
        buttonNo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                customAttachmentsDialog.dismiss();
            }
        });

        Button buttonYes = (Button) customAttachmentsDialog.findViewById(R.id.btn_driver_hire_dialog_hire);
        buttonYes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (hashMap.size() > 2) {
                    customAttachmentsDialog.dismiss();
                    etRegisterAttachments.setText("Documents 3/3");
                    etRegisterAttachments.setCompoundDrawablesWithIntrinsicBounds(R.mipmap.ic_edit_text_attachment, 0, R.mipmap.ic_edit_text_ok, 0);
                } else {
                    Toast.makeText(getActivity(), "Add all photos to continue", Toast.LENGTH_SHORT).show();
                }
            }
        });

        ibPhotoOne = (ImageButton) customAttachmentsDialog.findViewById(R.id.ib_photo_one);
        ibPhotoTwo = (ImageButton) customAttachmentsDialog.findViewById(R.id.ib_photo_two);
        ibPhotoThree = (ImageButton) customAttachmentsDialog.findViewById(R.id.ib_photo_three);

        ibPhotoOne.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ibPosition = 0;
                Helpers.AlertDialogWithPositiveNegativeFunctionsNeutralButton(getActivity(), "License Front",
                        "Select an option to add photo", "Camera", "Gallery", "Cancel", openCameraIntent, openGalleryIntent);
            }
        });

        ibPhotoTwo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ibPosition = 1;
                Helpers.AlertDialogWithPositiveNegativeFunctionsNeutralButton(getActivity(), "License Back",
                        "Select an option to add photo", "Camera", "Gallery", "Cancel", openCameraIntent, openGalleryIntent);
            }
        });

        ibPhotoThree.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ibPosition = 2;
                Helpers.AlertDialogWithPositiveNegativeFunctionsNeutralButton(getActivity(), "Police Verification",
                        "Select an option to add photo", "Camera", "Gallery", "Cancel", openCameraIntent, openGalleryIntent);
            }
        });

        if (etRegisterAttachments.getText().toString().equals("Documents 3/3")) {
            ibPhotoOne.setBackgroundDrawable(null);
            ibPhotoOne.setImageBitmap(Helpers.getResizedBitmapToDisplay(Helpers.getCroppedBitmap(BitmapFactory.decodeFile(hashMap.get(0))), 120));
            ibPhotoTwo.setBackgroundDrawable(null);
            ibPhotoTwo.setImageBitmap(Helpers.getResizedBitmapToDisplay(Helpers.getCroppedBitmap(BitmapFactory.decodeFile(hashMap.get(1))), 120));
            ibPhotoThree.setBackgroundDrawable(null);
            ibPhotoThree.setImageBitmap(Helpers.getResizedBitmapToDisplay(Helpers.getCroppedBitmap(BitmapFactory.decodeFile(hashMap.get(2))), 120));
        }

        customAttachmentsDialog.show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == PICK_IMAGE)
                new ConvertImage().execute(data);
            else if (requestCode == CAPTURE_IMAGE)
                onCaptureImageResult();
        }
    }

    private void onCaptureImageResult() {
        File file = new File(Environment.getExternalStorageDirectory() + File.separator +
                "image.jpg");
        Bitmap bm = Helpers.decodeBitmapFromFile(file.getAbsolutePath());
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        if (bm.getHeight() > 3200 || bm.getWidth() > 3200) {
            Bitmap.createScaledBitmap(bm, bm.getWidth() / 4, bm.getHeight() / 4, false).compress(Bitmap.CompressFormat.JPEG, 40, bytes);
        } else if (bm.getHeight() > 2560 || bm.getWidth() > 2560) {
            Bitmap.createScaledBitmap(bm, bm.getWidth() / 3, bm.getHeight() / 3, false).compress(Bitmap.CompressFormat.JPEG, 40, bytes);
        } else if (bm.getHeight() > 1600 || bm.getWidth() > 1600) {
            Bitmap.createScaledBitmap(bm, bm.getWidth() / 2, bm.getHeight() / 2, false).compress(Bitmap.CompressFormat.JPEG, 40, bytes);
        } else {
            bm.compress(Bitmap.CompressFormat.JPEG, 60, bytes);
        }
        hashMap.put(ibPosition, writeImageToExternalStorage(bytes, String.valueOf(ibPosition)));
        imagePathsArray.add(hashMap);
        if (ibPosition == 0) {
            ibPhotoOne.setBackgroundDrawable(null);
            ibPhotoOne.setImageBitmap(Helpers.getCroppedBitmap(Helpers.getResizedBitmapToDisplay(bm, 120)));
        } else if (ibPosition == 1) {
            ibPhotoTwo.setBackgroundDrawable(null);
            ibPhotoTwo.setImageBitmap(Helpers.getCroppedBitmap(Helpers.getResizedBitmapToDisplay(bm, 120)));
        } else if (ibPosition == 2) {
            ibPhotoThree.setBackgroundDrawable(null);
            ibPhotoThree.setImageBitmap(Helpers.getCroppedBitmap(Helpers.getResizedBitmapToDisplay(bm, 120)));
        }
    }

    private String writeImageToExternalStorage(ByteArrayOutputStream bytes, String name) {
        File destination = new File(Environment.getExternalStorageDirectory() + File.separator
                + "Android/data" + File.separator + AppGlobals.getContext().getPackageName());
        if (!destination.exists()) {
            destination.mkdirs();
        }
        File file = new File(destination, name + ".jpg");
        FileOutputStream fo;
        try {
            file.createNewFile();
            fo = new FileOutputStream(file);
            fo.write(bytes.toByteArray());
            fo.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return file.getAbsolutePath();
    }

    private class RegisterUserTask extends AsyncTask<Void, Integer, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Helpers.showProgressDialog(getActivity(), "Registering");
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                URL url;
                if (registerUserType == 0) {
                    url = new URL(EndPoints.REGISTER_CUSTOMER);
                } else {
                    url = new URL(EndPoints.REGISTER_DRIVER);
                }
                connection = (HttpURLConnection) url.openConnection();
                connection.setDoOutput(true);
                connection.setDoInput(true);
                connection.setInstanceFollowRedirects(false);
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("charset", "utf-8");
                DataOutputStream out = new DataOutputStream(connection.getOutputStream());
                if (registerUserType == 0) {
                    String dataForRegistration = getRegistrationStringForCustomer(userRegisterFullName,
                            userRegisterEmail, userRegisterPassword, userRegisterContactNumber, transmissionType, userRegisterVehicleType,
                            userRegisterVehicleMake, userRegisterVehicleModel);
                    Log.i("DataToWrite: ", "Customer: " + dataForRegistration);
                    out.writeBytes(dataForRegistration);
                    out.flush();
                    out.close();
                    responseCode = connection.getResponseCode();
                } else {
                    MultipartDataUtility http;
                    try {
                        http = new MultipartDataUtility(url);

                        http.addFormField("full_name", userRegisterFullName);
                        http.addFormField("email", userRegisterEmail);
                        http.addFormField("password", userRegisterPassword);
                        http.addFormField("phone_number", userRegisterContactNumber);
                        http.addFormField("transmission_type", String.valueOf(transmissionType));
                        http.addFormField("driving_experience", userRegisterDrivingExperience);
                        if (userRegisterBasicBio != null || !userRegisterBasicBio.equals("")) {
                            http.addFormField("bio", userRegisterBasicBio);
                        }
                        if (driverLocationToString != null) {
                            http.addFormField("location", driverLocationToString);
                        }
                        int doc = 1;
                        for (HashMap<Integer, String> item : imagePathsArray) {
                            System.out.println(item);
                            File file = new File(item.get(doc - 1));
                            http.addFilePart(("doc" + doc), file);
                            doc++;
                        }
                        final byte[] bytes = http.finish();
                        int counter = 0;
                        for (HashMap<Integer, String> item : imagePathsArray) {
                            try {
                                OutputStream os = new FileOutputStream(item.get(counter));
                                os.write(bytes);
                                counter++;
                            } catch (IOException e) {

                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            Helpers.dismissProgressDialog();
            if (responseCode == 201) {
                onRegistrationSuccess();
            } else if (responseCode == 400) {
                onRegistrationFailed("Registration failed. Email already in use");
            } else {
                onRegistrationFailed("Registration failed. Check internet and retry");
            }
        }
    }

    class ConvertImage extends AsyncTask<Intent, String, Bitmap> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Helpers.showProgressDialog(getActivity(), "Loading Image");
        }

        @Override
        protected Bitmap doInBackground(Intent... params) {
            Bitmap bm = null;
            if (params[0] != null) {
                try {
                    bm = MediaStore.Images.Media.getBitmap(getActivity().getContentResolver(), params[0].getData());
                    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                    if (bm.getHeight() > 3200 || bm.getWidth() > 3200) {
                        Bitmap.createScaledBitmap(bm, bm.getWidth() / 4, bm.getHeight() / 4, false).compress(Bitmap.CompressFormat.JPEG, 40, bytes);
                    } else if (bm.getHeight() > 2560 || bm.getWidth() > 2560) {
                        Bitmap.createScaledBitmap(bm, bm.getWidth() / 3, bm.getHeight() / 3, false).compress(Bitmap.CompressFormat.JPEG, 40, bytes);
                    } else if (bm.getHeight() > 1600 || bm.getWidth() > 1600) {
                        Bitmap.createScaledBitmap(bm, bm.getWidth() / 2, bm.getHeight() / 2, false).compress(Bitmap.CompressFormat.JPEG, 40, bytes);
                    } else {
                        bm.compress(Bitmap.CompressFormat.JPEG, 60, bytes);
                    }
                    hashMap.put(ibPosition, writeImageToExternalStorage(bytes, String.valueOf(ibPosition)));
                    imagePathsArray.add(hashMap);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return bm;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);
            Helpers.dismissProgressDialog();
            if (ibPosition == 0) {
                ibPhotoOne.setBackgroundDrawable(null);
                ibPhotoOne.setImageBitmap(Helpers.getCroppedBitmap(Helpers.getResizedBitmapToDisplay(bitmap, 120)));
            } else if (ibPosition == 1) {
                ibPhotoTwo.setBackgroundDrawable(null);
                ibPhotoTwo.setImageBitmap(Helpers.getCroppedBitmap(Helpers.getResizedBitmapToDisplay(bitmap, 120)));
            } else if (ibPosition == 2) {
                ibPhotoThree.setBackgroundDrawable(null);
                ibPhotoThree.setImageBitmap(Helpers.getCroppedBitmap(Helpers.getResizedBitmapToDisplay(bitmap, 120)));
            }
        }
    }
}