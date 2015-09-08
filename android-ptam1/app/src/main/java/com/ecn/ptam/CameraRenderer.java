package com.ecn.ptam;

import android.graphics.SurfaceTexture;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static android.opengl.GLES11.GL_ARRAY_BUFFER;
import static android.opengl.GLES11.GL_CLAMP_TO_EDGE;
import static android.opengl.GLES11.GL_FLOAT;
import static android.opengl.GLES11.GL_LINEAR;
import static android.opengl.GLES11.GL_STATIC_DRAW;
import static android.opengl.GLES11.GL_TEXTURE_COORD_ARRAY;
import static android.opengl.GLES11.GL_TEXTURE_MAG_FILTER;
import static android.opengl.GLES11.GL_TEXTURE_MIN_FILTER;
import static android.opengl.GLES11.GL_TEXTURE_WRAP_S;
import static android.opengl.GLES11.GL_TEXTURE_WRAP_T;
import static android.opengl.GLES11.GL_TRIANGLE_STRIP;
import static android.opengl.GLES11.GL_VERTEX_ARRAY;
import static android.opengl.GLES11.glBindBuffer;
import static android.opengl.GLES11.glBindTexture;
import static android.opengl.GLES11.glBufferData;
import static android.opengl.GLES11.glDisable;
import static android.opengl.GLES11.glDisableClientState;
import static android.opengl.GLES11.glDrawArrays;
import static android.opengl.GLES11.glEnable;
import static android.opengl.GLES11.glEnableClientState;
import static android.opengl.GLES11.glGenBuffers;
import static android.opengl.GLES11.glGenTextures;
import static android.opengl.GLES11.glTexCoordPointer;
import static android.opengl.GLES11.glTexParameterf;
import static android.opengl.GLES11.glTexParameteri;
import static android.opengl.GLES11.glVertexPointer;
import static android.opengl.GLES11Ext.GL_TEXTURE_EXTERNAL_OES;


/*
 * Renders the camera frame as an OpenGL texture
 */
public class CameraRenderer extends GLRenderer {

	private VideoSource _vs;
	private SurfaceTexture _tex;
	private IntBuffer _id;
	private IntBuffer _tex_id;
		
	public CameraRenderer(VideoSource vs) {
		_vs = vs;
	}
	
	public void draw() {
		_tex.updateTexImage();
		;
		
		glEnable(GL_TEXTURE_EXTERNAL_OES);
		glBindTexture(GL_TEXTURE_EXTERNAL_OES, _tex_id.get(0));
		glEnableClientState(GL_TEXTURE_COORD_ARRAY);
		glEnableClientState(GL_VERTEX_ARRAY);
		
		glBindBuffer(GL_ARRAY_BUFFER, _id.get(0));
		glVertexPointer(2, GL_FLOAT, 0, 0);
		glBindBuffer(GL_ARRAY_BUFFER, _id.get(1));
		glTexCoordPointer(2, GL_FLOAT, 0, 0);
		
		glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
		
		glBindBuffer(GL_ARRAY_BUFFER, 0);
		glBindTexture(GL_TEXTURE_EXTERNAL_OES, 0);
		glDisableClientState(GL_TEXTURE_COORD_ARRAY);
		glDisableClientState(GL_VERTEX_ARRAY);
		glDisable(GL_TEXTURE_EXTERNAL_OES);
	}
	
	public void changed(int width, int height) {
		_vs.set_texture(_tex);
	}
	
	public void init() {
		_tex_id = IntBuffer.allocate(1);
		glGenTextures(1, _tex_id);
		
		glEnable(GL_TEXTURE_EXTERNAL_OES);
		glBindTexture(GL_TEXTURE_EXTERNAL_OES, _tex_id.get(0));
		glTexParameterf(GL_TEXTURE_EXTERNAL_OES, GL_TEXTURE_MIN_FILTER,GL_LINEAR);        
		glTexParameterf(GL_TEXTURE_EXTERNAL_OES, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
		glTexParameteri(GL_TEXTURE_EXTERNAL_OES, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
		glTexParameteri(GL_TEXTURE_EXTERNAL_OES, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

		_tex = new SurfaceTexture(_tex_id.get(0));
		
		_id = IntBuffer.allocate(2);
		glGenBuffers(2, _id);
		
		FloatBuffer tex_array = FloatBuffer.wrap(new float[] {
				-1.f,-1.f, -1.f,1.f, 1.f,-1.f, 1.f,1.f});
		glBindBuffer(GL_ARRAY_BUFFER, _id.get(0));
		glBufferData(GL_ARRAY_BUFFER, tex_array.capacity()*4, tex_array, GL_STATIC_DRAW);
		glBindBuffer(GL_ARRAY_BUFFER, 0);
		
		FloatBuffer tex_coord = FloatBuffer.wrap(new float[] {
				0,1, 0,0, 1,1, 1,0});
		glBindBuffer(GL_ARRAY_BUFFER, _id.get(1));
		glBufferData(GL_ARRAY_BUFFER, tex_coord.capacity()*4, tex_coord, GL_STATIC_DRAW);
		glBindBuffer(GL_ARRAY_BUFFER, 0);
	}

}