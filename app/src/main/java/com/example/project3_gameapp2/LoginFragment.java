package com.example.project3_gameapp2;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.project3_gameapp2.R.databinding.FragmentLoginBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;


public class LoginFragment extends Fragment {

    private static final String TAG = "login fragment";
    LoginFragmentListener mListener;
    FragmentLoginBinding binding;
    private FirebaseAuth mAuth = FirebaseAuth.getInstance();

    public LoginFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentLoginBinding.inflate(inflater, container, false);
        getActivity().findViewById(R.id.toolbar).setVisibility(View.GONE);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.buttonLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String userEmail = binding.editTextTextEmailAddress.getText().toString();
                String userPassword = binding.editTextTextPassword.getText().toString();

                if(userEmail.isEmpty()){
                    Toast.makeText(getActivity().getApplicationContext(), "Email is required", Toast.LENGTH_SHORT).show();
                } else if (userPassword.isEmpty()){
                    Toast.makeText(getActivity().getApplicationContext(), "Password is required", Toast.LENGTH_SHORT).show();
                } else {
                    mAuth.signInWithEmailAndPassword(userEmail, userPassword)
                            .addOnCompleteListener(getActivity(), new OnCompleteListener<AuthResult>() {
                                @Override
                                public void onComplete(@NonNull Task<AuthResult> task) {
                                    if (task.isSuccessful()) {
                                        Log.d(TAG, "onComplete: Logged In Successfully");
                                        FirebaseUser user = mAuth.getCurrentUser();
                                        Log.d(TAG, "onComplete: User " + user);
                                        //mListener.goToChatrooms();
                                    } else {
                                        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                                        builder.setTitle("Login Error")
                                                .setMessage(task.getException().getMessage())
                                                .setNeutralButton("Ok", new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(DialogInterface dialogInterface, int i) {
                                                        Log.d(TAG, "onClick: Ok clicked");
                                                    }
                                                });
                                        builder.create().show();
                                        Log.d(TAG, "onComplete: Login Error" + task.getException().getMessage());
                                    }
                                }
                            });
                }
            }
        });

        binding.buttonRegistration.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mListener.goToRegistration();
            }
        });
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mListener = (LoginFragmentListener) context;
    }

    public interface LoginFragmentListener {
        void goToRegistration();
    }
}