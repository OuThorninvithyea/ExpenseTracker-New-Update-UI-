# Database Testing Guide

## Steps to Test Login/Signup:

1. **Clear App Data:**
   - Go to Settings > Apps > Your App > Storage > Clear Data
   - Or uninstall and reinstall the app

2. **Check Logcat:**
   - Filter by "DatabaseHelper" or "DataManager"
   - Look for these messages:
     - "DatabaseHelper constructor called"
     - "Creating database tables..."
     - "Users table created"
     - "Database created successfully"
     - "Signup attempt for username: ..."
     - "Login attempt for user: ..."

3. **Test Signup:**
   - Enter username: testuser
   - Enter password: test123 (at least 3 characters)
   - Enter security answer: dog
   - Click Sign Up
   - Check Logcat for "Signup successful" or error messages

4. **Test Login:**
   - Enter the same username and password
   - Click Log In
   - Check Logcat for "Login successful" or error messages

## Common Issues:

- If you see "Users table does not exist": Database wasn't created properly
- If you see "Hash generation failed": Password hashing issue
- If you see "Username already exists": Try a different username
- If you see "Invalid username or password": Check if you're using the correct credentials

## Debug Commands:

Check Logcat in Android Studio:
```
adb logcat | grep -E "DatabaseHelper|DataManager"
```
