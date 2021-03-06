package com.xupz.manhuareade.ui.fragment;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentTransaction;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;


import com.xupz.manhuareade.R;
import com.xupz.manhuareade.database.ComicDbHelper;
import com.xupz.manhuareade.database.PasswordDbHelper;
import com.xupz.manhuareade.model.ComicBook;
import com.xupz.manhuareade.model.Password;
import com.xupz.manhuareade.ui.activity.BookCollectionActivity;
import com.xupz.manhuareade.util.ZipCommandUtil;
import com.xupz.manhuareade.util.ZipProcess;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by YuZhicong on 2017/5/7.
 */

public class DecompressPasswordInputFragment extends DialogFragment {

    private Dialog dialog;
    private EditText etDCpassword;
    private CheckBox cbUsePasswordLibrary;
    private String dirPath;
    private BookCollectionActivity mActivity;
    private List<Password> lists;
    private int index = 0;
    private ComicDbHelper helper;
    private String outputPath;
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(getActivity(), R.style.AppThemeDialog);
        View contentView = getActivity().getLayoutInflater().inflate(R.layout.fragment_input_compresspassword, null, false);

        etDCpassword = (EditText) contentView.findViewById(R.id.etDCpassword);
        cbUsePasswordLibrary = (CheckBox) contentView.findViewById(R.id.cbUsePasswordLibrary);

        helper = ComicDbHelper.getComicDBHelper(getActivity());
        dirPath = (String) getArguments().getCharSequence("dirPath");
        outputPath = getActivity().getExternalCacheDir().getAbsolutePath() + "/.Comic/" + ComicBook.parseBookname(dirPath);

        mActivity = (BookCollectionActivity) getActivity();

        Log.e("DecompressPassword",mActivity.toString());

        dialog = builder.setView(contentView)
                //.setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(R.string.this_comic_file_need_password)
                .setCancelable(true)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                        if (i == DialogInterface.BUTTON_POSITIVE) {
                            if(!cbUsePasswordLibrary.isChecked()){
                                if(TextUtils.isEmpty(etDCpassword.getText().toString())){
                                    etDCpassword.setError(getString(R.string.password_empty_message));
                                }else{
                                    //?????????????????????
                                    FragmentTransaction mFragmentTransaction = getFragmentManager().beginTransaction();
                                    mFragmentTransaction.remove(DecompressPasswordInputFragment.this);
                                    mFragmentTransaction.addToBackStack(null);
                                    mFragmentTransaction.commit();
                                    lists = new ArrayList<Password>();
                                    Password password = new Password();
                                    password.setPassword(etDCpassword.getText().toString());
                                    lists.add(password);
                                    index = 0;
                                    DecompressComicfile();
                                }
                            }else{
                                //???????????????????????????
                                lists = PasswordDbHelper.getPasswordDbHelper(getActivity()).queryPasswords();
                                index = 0;
                                DecompressComicfile();
                            }
                        }

                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .create();
        dialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN) {
                    dismiss();
                }
                return false;
            }
        });


        return dialog;
    }

    @Override
    public void onStart() {
        super.onStart();
        DisplayMetrics dm = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(dm);
        getDialog().getWindow().setLayout(dm.widthPixels, getDialog().getWindow().getAttributes().height);
    }

    private boolean isSuccess = false;

    void DecompressComicfile(){

            Password password = lists.get(index);
            String decompressCommand = ZipCommandUtil.getDecompressCommand(dirPath,
                    outputPath,true,password.getPassword(),ZipCommandUtil.OverWriteMode.OVERWRITE_ALL_EXISTING_FILE,null);

            Log.e("Decompress:",decompressCommand);
            Log.e("UsePassword: ",password.getPassword());
            ZipProcess zipProcess = new ZipProcess(mActivity,decompressCommand,dirPath);

            zipProcess.setZipProcessListener(listenner);
            index++;
            zipProcess.start();//????????????

    }

    private ZipProcess.ZipProcessListenner listenner = new ZipProcess.ZipProcessListenner() {
        @Override
        public void onZipSuccess(final ProgressDialog dialog) {//??????????????????ui?????????????????????
            dialog.setTitle(mActivity.getResources().getString(R.string.analyse_title));
            dialog.setMessage(mActivity.getResources().getString(R.string.analyse_message));
            new Thread(new Runnable() {
                @Override
                public void run() {
                    final boolean canInsert = helper.insertComicBook(new ComicBook(getActivity(),dirPath,outputPath,""));
                    dialog.dismiss();
                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if(canInsert){
                                mActivity.adapter.refreshBookList(null);
                                mActivity.showMessage(mActivity.getResources().getString(R.string.add_book_success));
                            }else{
                                mActivity.showMessage(mActivity.getResources().getString(R.string.add_book_failure));
                            }
                            isSuccess = true;
                        }
                    });
                }
            }).start();

        }

        @Override
        public void onZipFault(ProgressDialog dialog) {
            dialog.dismiss();
            if(index == lists.size()) {
                //getFragmentManager().popBackStack(); ????????????????????????????????????????????? ?????????????????? ???????????????
                mActivity.showMessage(mActivity.getString(R.string.no_matching_password));

            }else{
                //???????????????????????????????????????
                DecompressComicfile();
            }
        }
    };
}
