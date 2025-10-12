using UnityEngine;
using UnityEngine.Networking;
using TMPro;
using System.Collections;
using System.Collections.Generic;
using UnityEngine.UI;

public class CodeChallengeNPC : MonoBehaviour
{
    [Header("UI References")]
    public TMP_InputField playerCodeInput;
    public TMP_Text challengeText;
    public TMP_Text responseText;
    public GameObject CodeChallengePanel;
    public Button submitButton;
    public Button cancelButton;

    [Header("Settings")]
    public string serverUrl = "https://codechallengeserver-dtim.onrender.com/run";
    public GameObject gate;

    private string currentTemplate;
    private string correctAnswer;

    private void Start()
    {
        if (cancelButton != null)
            cancelButton.onClick.AddListener(OnCancel);
    }

    public void OpenPanel()
    {
        GenerateRandomChallenge();

        playerCodeInput.text = "";
        responseText.text = "";
        playerCodeInput.interactable = true;

        if (CodeChallengePanel != null)
            CodeChallengePanel.SetActive(true);

        if (submitButton != null)
            submitButton.interactable = true;
    }

    // 🧠 Generate a random challenge
    void GenerateRandomChallenge()
    {
        int randomIndex = Random.Range(0, 4);
        switch (randomIndex)
        {
            case 0:
                currentTemplate = @"____ class Test {
    public static void main(String[] args) {
        System.out.println(""Hello, NPC!"");
    }
}";
                correctAnswer = "public";
                break;

            case 1:
                currentTemplate = @"public class ____ {
    public static void main(String[] args) {
        System.out.println(""Hello, NPC!"");
    }
}";
                correctAnswer = "Test";
                break;

            case 2:
                currentTemplate = @"public class Test {
    ____ static void main(String[] args) {
        System.out.println(""Hello, NPC!"");
    }
}";
                correctAnswer = "public";
                break;

            case 3:
                currentTemplate = @"public class Test {
    public static void main(String[] args) {
        System.out.println(""____, NPC!"");
    }
}";
                correctAnswer = "Hello";
                break;
        }

        challengeText.text = currentTemplate;
    }

    public void OnSubmitCode()
    {
        string userAnswer = playerCodeInput.text.Trim();

        if (string.IsNullOrEmpty(userAnswer))
        {
            responseText.text = "⚠️ Please type your answer first!";
            return;
        }

        // Replace the blank with player's answer
        string completedCode = currentTemplate.Replace("____", userAnswer);

        // Disable input during check
        playerCodeInput.interactable = false;
        if (submitButton != null)
            submitButton.interactable = false;

        responseText.text = "⏳ Checking your code...";
        StartCoroutine(SendCodeToServer(completedCode));
    }

    IEnumerator SendCodeToServer(string code)
    {
        WWWForm form = new WWWForm();
        form.AddField("code", code);

        using (UnityWebRequest request = UnityWebRequest.Post(serverUrl, form))
        {
            yield return request.SendWebRequest();

            if (request.result != UnityWebRequest.Result.Success)
            {
                responseText.text = "❌ Error: " + request.error;
                EnableRetry();
            }
            else
            {
                string serverResponse = request.downloadHandler.text;
                Debug.Log("Server response: " + serverResponse);

                if (serverResponse.Contains("Hello, NPC!"))
                {
                    responseText.text = "✅ Correct! You may enter.";
                    if (gate != null)
                        gate.SetActive(false);

                    yield return new WaitForSeconds(1f);
                    CodeChallengePanel.SetActive(false);
                }
                else
                {
                    responseText.text = "❌ Incorrect. Try again!";
                    EnableRetry();
                }
            }
        }
    }

    private void EnableRetry()
    {
        playerCodeInput.text = "";
        playerCodeInput.interactable = true;
        if (submitButton != null)
            submitButton.interactable = true;
    }

    public void OnCancel()
    {
        CodeChallengePanel.SetActive(false);
    }
}
