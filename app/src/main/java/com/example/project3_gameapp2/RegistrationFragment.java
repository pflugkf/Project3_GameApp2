package com.example.project3_gameapp2;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.project3_gameapp2.databinding.FragmentRegistrationBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;


public class RegistrationFragment extends Fragment {
    private static final String TAG = "registration fragment";
    RegistrationFragmentListener mListener;
    FragmentRegistrationBinding binding;
    private FirebaseAuth mAuth = FirebaseAuth.getInstance();

    public RegistrationFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentRegistrationBinding.inflate(inflater, container, false);
        getActivity().findViewById(R.id.toolbar).setVisibility(View.GONE);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getActivity().setTitle(R.string.register_fragment);

        binding.buttonRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String firstName = binding.editTextRegisterFirstName.getText().toString();
                String lastName = binding.editTextRegisterLastName.getText().toString();
                String email = binding.editTextRegisterEmail.getText().toString();
                String password = binding.editTextRegisterPassword.getText().toString();

                if (firstName.trim().isEmpty()){
                    Toast.makeText(getActivity().getApplicationContext(), "First name is required", Toast.LENGTH_SHORT).show();
                } else if (lastName.trim().isEmpty()) {
                    Toast.makeText(getActivity().getApplicationContext(), "Last name is required", Toast.LENGTH_SHORT).show();
                } else if (email.trim().isEmpty()) {
                    Toast.makeText(getActivity().getApplicationContext(), "Email is required", Toast.LENGTH_SHORT).show();
                } else if (password.trim().isEmpty()) {
                    Toast.makeText(getActivity().getApplicationContext(), "Password is required", Toast.LENGTH_SHORT).show();
                } else {
                    mAuth.createUserWithEmailAndPassword(email,password)
                            .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                                @Override
                                public void onComplete(@NonNull Task<AuthResult> task) {
                                    if(task.isSuccessful()){
                                        Log.d(TAG, "onComplete: Registration is Successful");
                                        FirebaseUser user = mAuth.getCurrentUser();
                                        Log.d(TAG, "onComplete: User " + user);

                                        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                                .setDisplayName(firstName + " " + lastName)
                                                .build();

                                        user.updateProfile(profileUpdates)
                                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                    @Override
                                                    public void onComplete(@NonNull Task<Void> task) {
                                                        if (task.isSuccessful()) {
                                                            FirebaseUser updatedUser = mAuth.getCurrentUser();
                                                            Log.d(TAG, "User Display Name = " + updatedUser.getDisplayName());
                                                            createUser();
                                                        }
                                                    }
                                                });

                                        //mListener.goToChatrooms();
                                    } else {
                                        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                                        builder.setTitle("Registration  Error")
                                                .setMessage(task.getException().getMessage())
                                                .setNeutralButton("Ok", new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(DialogInterface dialogInterface, int i) {
                                                        Log.d(TAG, "onClick: Ok clicked");
                                                    }
                                                });
                                        builder.create().show();
                                        Log.d(TAG, "onComplete: Registration Error" + task.getException().getMessage());
                                    }
                                }
                            });
                }
            }
        });

        binding.buttonCancelToLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mListener.backToLogin();
            }
        });
    }

    private void createUser() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        FirebaseUser user = mAuth.getCurrentUser();
        String id = user.getUid();
        String email = user.getEmail();
        String firstName = binding.editTextRegisterFirstName.getText().toString();
        String lastName = binding.editTextRegisterLastName.getText().toString();


        HashMap<String, Object> newUser = new HashMap<>();

        newUser.put("id", id);
        newUser.put("email", email);
        newUser.put("firstName", firstName);
        newUser.put("lastName", lastName);

        db.collection("users")
                .document(id)
                .set(newUser)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Log.d(TAG, "New user was successfully added!");
                        Log.d(TAG, "onSuccess: " + newUser);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(TAG, "Error creating new user" + e);
                    }
                });

    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mListener = (RegistrationFragmentListener) context;
    }

    interface RegistrationFragmentListener {
        void backToLogin();
    }
}