interface User {
  _id: string;
  username: string;
  email: string;
  password?: string;
  profileImage?: string;
  createdAt: Date;
  updatedAt: Date;
  googleId?: string;
  facebookId?: string;
}

export default User; 