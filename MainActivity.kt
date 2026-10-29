package com.cacau.r1

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private lateinit var web: WebView
    private val worker = Executors.newSingleThreadExecutor()
    private val prefs by lazy {
        val master = MasterKey.Builder(this).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
        EncryptedSharedPreferences.create(this, "cacau-secure", master,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM)
    }
    private fun p(k:String, d:String="") = prefs.getString(k,d) ?: d
    private fun save(k:String,v:String) { prefs.edit().putString(k,v).apply() }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        web = WebView(this)
        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        web.settings.mediaPlaybackRequiresUserGesture = false
        web.settings.allowFileAccess = true
        web.settings.allowContentAccess = false
        web.settings.allowFileAccessFromFileURLs = false
        web.settings.allowUniversalAccessFromFileURLs = false
        web.webChromeClient = WebChromeClient()
        web.webViewClient = WebViewClient()
        web.addJavascriptInterface(Bridge(), "CacauNative")
        setContentView(web)
        web.loadUrl("file:///android_asset/index.html")
    }
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu.add(0,1,0,"Configurar Gemini"); menu.add(0,2,1,"Conectar Spotify"); menu.add(0,3,2,"Configurar Google / OAuth"); menu.add(0,4,3,"Status das integrações")
        return true
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean { when(item.itemId){1->configureGemini();2->configureSpotify();3->googleHelp();4->statusDialog();else->return super.onOptionsItemSelected(item)};return true }
    private fun field(hint:String, value:String="", password:Boolean=false): EditText = EditText(this).apply { this.hint=hint; setText(value); if(password) inputType=129 }
    private fun configureGemini() {
        val box=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(48,8,48,0) }
        box.addView(TextView(this).apply { text="Cole a chave da API Gemini. Ela fica criptografada neste aparelho, mas chamadas diretas do app não são a melhor opção para distribuir publicamente." })
        val key=field("Gemini API key / auth key",p("gemini_key"),true);box.addView(key)
        AlertDialog.Builder(this).setTitle("Gemini online").setView(box).setNegativeButton("Cancelar",null).setNeutralButton("Apagar") { _,_->save("gemini_key","") }.setPositiveButton("Salvar") { _,_->save("gemini_key",key.text.toString().trim());toast("Configuração salva") }.show()
    }
    private fun configureSpotify() {
        val box=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(48,8,48,0) }
        box.addView(TextView(this).apply { text="No Spotify Developer Dashboard crie um app e cadastre exatamente este Redirect URI: cacaur1://spotify-callback. Depois cole o Client ID." })
        val id=field("Spotify Client ID",p("spotify_client_id"));box.addView(id)
        val red=field("Redirect URI",p("spotify_redirect","cacaur1://spotify-callback"));box.addView(red)
        AlertDialog.Builder(this).setTitle("Spotify").setView(box).setNegativeButton("Cancelar",null).setPositiveButton("Salvar e entrar") { _,_->save("spotify_client_id",id.text.toString().trim());save("spotify_redirect",red.text.toString().trim());spotifyLogin() }.show()
    }
    private fun googleHelp() { AlertDialog.Builder(this).setTitle("Google Cloud / OAuth").setMessage("Próximo passo: crie um projeto em console.cloud.google.com, ative apenas as APIs que você quer usar e configure a tela de consentimento OAuth. Gmail, Agenda, Drive, Maps e YouTube têm permissões separadas. Este primeiro pacote ainda não contém conectores de dados Google; não exibirá dados reais até implementarmos e autorizarmos cada API.").setPositiveButton("Abrir Google Cloud") { _,_->startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("https://console.cloud.google.com/"))) }.setNegativeButton("Fechar",null).show() }
    private fun statusDialog() { AlertDialog.Builder(this).setTitle("Cacau R1 · integrações").setMessage("Gemini: ${if(p("gemini_key").isNotBlank()) "chave configurada" else "precisa configurar"}\nSpotify: ${if(p("spotify_client_id").isNotBlank()) "Client ID configurado" else "precisa configurar"}\nSpotify autorizado: ${if(p("spotify_access").isNotBlank()) "sim (token pode expirar)" else "não"}\nGoogle Workspace: não conectado nesta versão\nOffline: comandos locais do HTML funcionam; modelo generativo offline ainda não incluído.").setPositiveButton("OK",null).show() }
    private fun toast(s:String) = runOnUiThread { android.widget.Toast.makeText(this,s,android.widget.Toast.LENGTH_LONG).show() }

    private fun b64(bytes:ByteArray)=Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    private fun spotifyLogin() {
        val client=p("spotify_client_id"); if(client.isBlank()){toast("Informe o Client ID do Spotify");return}
        val verifier=b64(ByteArray(64).also{SecureRandom().nextBytes(it)})
        val challenge=b64(MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.US_ASCII)))
        val state=b64(ByteArray(24).also{SecureRandom().nextBytes(it)})
        save("spotify_verifier",verifier);save("spotify_state",state)
        val scopes="user-read-currently-playing user-read-playback-state user-modify-playback-state"
        val uri=Uri.parse("https://accounts.spotify.com/authorize").buildUpon().appendQueryParameter("client_id",client).appendQueryParameter("response_type","code").appendQueryParameter("redirect_uri",p("spotify_redirect","cacaur1://spotify-callback")).appendQueryParameter("code_challenge_method","S256").appendQueryParameter("code_challenge",challenge).appendQueryParameter("state",state).appendQueryParameter("scope",scopes).build()
        startActivity(Intent(Intent.ACTION_VIEW,uri))
    }
    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); setIntent(intent); handleSpotifyRedirect(intent.data) }
    private fun handleSpotifyRedirect(uri:Uri?) { if(uri?.scheme!="cacaur1"||uri.host!="spotify-callback")return
        val state=uri.getQueryParameter("state");val code=uri.getQueryParameter("code")
        if(state!=p("spotify_state")||code.isNullOrBlank()){toast("Login Spotify cancelado ou state inválido");return}
        worker.execute { try { val data=listOf("grant_type" to "authorization_code","code" to code,"redirect_uri" to p("spotify_redirect","cacaur1://spotify-callback"),"client_id" to p("spotify_client_id"),"code_verifier" to p("spotify_verifier")).joinToString("&"){enc(it.first)+"="+enc(it.second)}
            val j=postForm("https://accounts.spotify.com/api/token",data);save("spotify_access",j.getString("access_token"));save("spotify_refresh",j.optString("refresh_token",p("spotify_refresh")));save("spotify_expiry",(System.currentTimeMillis()+j.optLong("expires_in",3600)*1000).toString());runOnUiThread{toast("Spotify conectado")}
        }catch(e:Exception){toast("Falha ao conectar Spotify: ${e.message}")} }
    }
    private fun enc(s:String)=URLEncoder.encode(s,"UTF-8")
    private fun postForm(url:String,body:String):JSONObject { val c=URL(url).openConnection() as HttpURLConnection;c.requestMethod="POST";c.doOutput=true;c.setRequestProperty("Content-Type","application/x-www-form-urlencoded");c.connectTimeout=15000;c.readTimeout=15000;c.outputStream.use{it.write(body.toByteArray())};val stream=if(c.responseCode in 200..299)c.inputStream else c.errorStream;val txt=stream.bufferedReader().use{it.readText()};if(c.responseCode !in 200..299)throw Exception(txt);return JSONObject(txt) }
    private fun spotifyRequest(path:String,method:String="GET"):JSONObject { val token=p("spotify_access");if(token.isBlank())throw Exception("Spotify não conectado");val c=URL("https://api.spotify.com/v1/$path").openConnection() as HttpURLConnection;c.requestMethod=method;c.setRequestProperty("Authorization","Bearer $token");c.connectTimeout=10000;c.readTimeout=10000;val code=c.responseCode;if(code==204)return JSONObject().put("empty",true);val stream=if(code in 200..299)c.inputStream else c.errorStream;val txt=stream.bufferedReader().use{it.readText()};if(code !in 200..299)throw Exception("Spotify HTTP $code: $txt");return JSONObject(txt) }
    inner class Bridge {
        @JavascriptInterface fun isGeminiConfigured()=p("gemini_key").isNotBlank()
        @JavascriptInterface fun isSpotifyConnected()=p("spotify_access").isNotBlank()
        @JavascriptInterface fun askGemini(prompt:String,id:String) { worker.execute { var answer="";try { val key=p("gemini_key");if(key.isBlank())throw Exception("Configure Gemini no menu ⋮");val body=JSONObject().put("contents",org.json.JSONArray().put(JSONObject().put("role","user").put("parts",org.json.JSONArray().put(JSONObject().put("text","Você é Cacau, uma calopsita assistente em português brasileiro. Seja útil, clara e concisa. Não afirme que executou ações externas sem confirmação. Pedido: $prompt"))))
                val conn=URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent").openConnection() as HttpURLConnection;conn.requestMethod="POST";conn.doOutput=true;conn.setRequestProperty("Content-Type","application/json");conn.setRequestProperty("x-goog-api-key",key);conn.connectTimeout=15000;conn.readTimeout=30000;conn.outputStream.use{it.write(body.toString().toByteArray(Charsets.UTF_8))};val code=conn.responseCode;val stream=if(code in 200..299)conn.inputStream else conn.errorStream;val raw=stream.bufferedReader().use{it.readText()};if(code !in 200..299)throw Exception("Gemini HTTP $code");answer=JSONObject(raw).getJSONArray("candidates").getJSONObject(0).getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text")
            }catch(e:Exception){answer="Não consegui acessar o Gemini: ${e.message}. Verifique a chave e a internet."};runOnUiThread{web.evaluateJavascript("window.CacauGeminiResult(${JSONObject.quote(id)},${JSONObject.quote(answer)});",null)} } }
        @JavascriptInterface fun spotifyAction(action:String) { worker.execute { try { val endpoint=when(action){"play"->"me/player/play";"pause"->"me/player/pause";"next"->"me/player/next";"previous"->"me/player/previous";else->return@execute};val method=if(action=="next"||action=="previous")"POST" else if(action=="play"||action=="pause")"PUT" else "GET";spotifyRequest(endpoint,method);toast("Spotify: $action enviado") }catch(e:Exception){toast("Spotify: ${e.message}")} } }
        @JavascriptInterface fun getSpotifyTrack(id:String) { worker.execute { try { val j=spotifyRequest("me/player/currently-playing");val item=j.optJSONObject("item");if(item!=null){val artists=item.optJSONArray("artists");val artist=if(artists!=null&&artists.length()>0)artists.getJSONObject(0).optString("name") else "";val out=JSONObject().put("name",item.optString("name")).put("artist",artist).put("is_playing",j.optBoolean("is_playing"));runOnUiThread{web.evaluateJavascript("window.CacauSpotifyTrackResult(${JSONObject.quote(id)},${JSONObject.quote(out.toString())});",null)}} }catch(_:Exception){} } }
    }
    override fun onDestroy(){web.removeJavascriptInterface("CacauNative");web.destroy();worker.shutdownNow();super.onDestroy()}
}
