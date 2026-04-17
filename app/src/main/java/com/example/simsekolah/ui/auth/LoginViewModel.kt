package com.example.simsekolah.ui.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.simsekolah.data.model.UserModel
import com.example.simsekolah.data.remote.repository.SchoolRepository
import kotlinx.coroutines.launch

class LoginViewModel(private val repository: SchoolRepository) : ViewModel() {

    private val _loginResult = MutableLiveData<Result<UserModel>>()
    val loginResult: LiveData<Result<UserModel>> = _loginResult

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    fun login(username: String, passwordInput: String, role: String) {
        if (username.isEmpty()) {
            _loginResult.value = Result.failure(Exception("Username tidak boleh kosong"))
            return
        }
        if (passwordInput.isEmpty()) {
            _loginResult.value = Result.failure(Exception("Password tidak boleh kosong"))
            return
        }

        _isLoading.value = true
        viewModelScope.launch {
            try {
                if (role == "guru") {
                    repository.getGuru().collect { response ->
                        if (response.success) {
                            val guru = response.data.find { (it.email == username || it.nip == username) && it.password == passwordInput }
                            if (guru != null) {
                                _loginResult.value = Result.success(UserModel(name = guru.nama, email = guru.email, role = "guru"))
                            } else {
                                _loginResult.value = Result.failure(Exception("Email/NIP atau password salah"))
                            }
                        }
                    }
                } else {
                    repository.getSiswa().collect { response ->
                        if (response.success) {
                            val murid = response.data.find { (it.email == username || it.nama.equals(username, ignoreCase = true)) && it.password == passwordInput }
                            if (murid != null) {
                                _loginResult.value = Result.success(UserModel(name = murid.nama, email = murid.email, role = "murid"))
                            } else {
                                _loginResult.value = Result.failure(Exception("Username atau password salah"))
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                _loginResult.value = Result.failure(e)
            } finally {
                _isLoading.value = false
            }
        }
    }
}
