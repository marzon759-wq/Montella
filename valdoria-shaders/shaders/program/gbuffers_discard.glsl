// The sky, sun, moon and vanilla clouds are drawn by Valdoria's own sky in deferred.
#ifdef VSH
void main() { gl_Position = vec4(-10.0); }
#endif
#ifdef FSH
void main() { discard; }
#endif
