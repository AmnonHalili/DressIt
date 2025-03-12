interface Post {
  _id: string;
  userId: string;
  text: string;
  imageUrl?: string;
  likes: string[]; // Array of user IDs who liked the post
  commentCount: number;
  createdAt: Date;
  updatedAt: Date;
}

export default Post; 